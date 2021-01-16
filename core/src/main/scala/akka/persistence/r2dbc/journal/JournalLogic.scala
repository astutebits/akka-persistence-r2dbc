/*
 * Copyright 2020-2021 Borislav Borisov.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package akka.persistence.r2dbc.journal

import akka.actor.ActorSystem
import akka.persistence.{AtomicWrite, PersistentRepr}
import akka.serialization.SerializationExtension
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import java.util.{HashMap => JHMap, Map => JMap}
import scala.collection.immutable.Seq
import scala.concurrent.duration.{FiniteDuration, NANOSECONDS}
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.Try

/**
 * A mixin with the journal logic (separated from [[ReactiveJournal]] to make testing easier).
 */
trait JournalLogic {

  implicit val system: ActorSystem

  lazy val reprSerDe = new PersistenceReprSerDe(SerializationExtension(system))

  private lazy implicit val mat: Materializer = Materializer(system)
  private lazy implicit val ec: ExecutionContextExecutor = system.dispatcher
  private lazy val replayTimeout = {
    val pluginPath = system.settings.config.getString("akka.persistence.journal.plugin")
    val duration = system.settings.config.getConfig(pluginPath).getDuration("replay-messages-timeout")
    FiniteDuration(duration.toNanos, NANOSECONDS)
  }

  protected val dao: JournalDao

  private val writeInProgress: JMap[String, Future[Any]] = new JHMap

  def asyncWriteMessages(messages: Seq[AtomicWrite]): Future[Seq[Try[Unit]]] = {
    val asyncSerialized: Seq[Future[Try[Seq[JournalEntry]]]] = messages.map { atomicWrite =>
      // Since all PersistentRepr are persisted atomically they all get the same timestamp
      val now = System.currentTimeMillis()
      val serialized = atomicWrite.payload.map(pr => reprSerDe.serialize(pr.withTimestamp(now)))
      Future.sequence(serialized).map(TryUtil.flatten)
    }

    val future = Source.future(Future.sequence(asyncSerialized))
        .flatMapConcat((serializedTries: Seq[Try[Seq[JournalEntry]]]) => {
          val entriesToWrite: Seq[JournalEntry] = for {
            serializeTry: Try[Seq[JournalEntry]] <- serializedTries
            row: JournalEntry <- serializeTry.getOrElse(Seq.empty)
          } yield row
          dao.writeEvents(entriesToWrite).map(_ => TryUtil.writeCompleteSignal(serializedTries))
        })
        .runWith(Sink.last)

    val pid = messages.head.persistenceId
    writeInProgress.put(pid, future)
    future.andThen { case _ => writeInProgress.remove(pid) }
  }

  def asyncDeleteMessagesTo(persistenceId: String, toSequenceNr: Long): Future[Unit] =
    dao.deleteEvents(persistenceId, toSequenceNr).map(_ => ()).runWith(Sink.last)

  def asyncReplayMessages(
      persistenceId: String,
      fromSequenceNr: Long,
      toSequenceNr: Long,
      max: Long
  )(recoveryCallback: PersistentRepr => Unit): Future[Unit] = {
    dao.fetchEvents(persistenceId, fromSequenceNr, toSequenceNr, max)
        .completionTimeout(replayTimeout)
        .mapAsync(1)(entry => reprSerDe.deserialize(entry).flatMap(Future.fromTry))
        .runForeach(repr => recoveryCallback(repr))
        .map(_ => ())
  }

  def asyncReadHighestSequenceNr(persistenceId: String, fromSequenceNr: Long): Future[Long] = {
    def go(persistenceId: String, fromSequenceNr: Long) =
      dao.readHighestSequenceNr(persistenceId, fromSequenceNr)
          .orElse(Source.single(0L))
          .runWith(Sink.last)

    writeInProgress.get(persistenceId) match {
      case null => go(persistenceId, fromSequenceNr)
      case f => f.flatMap(_ => go(persistenceId, fromSequenceNr))
    }
  }

}
