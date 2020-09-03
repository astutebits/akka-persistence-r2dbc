/*
 * Copyright 2020 Borislav Borisov.
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
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.Try
import scala.collection.immutable

/**
 * A mixin with the journal logic (separated from [[ReactiveJournal]] to make testing easier).
 */
trait JournalLogic {

  implicit val system: ActorSystem
  lazy val serializer = new PersistenceReprSerDe(SerializationExtension(system))

  private lazy implicit val mat: Materializer = Materializer(system)
  private lazy implicit val ec: ExecutionContextExecutor = system.dispatcher
  protected val dao: JournalDao
  private val writeInProgress: JMap[String, Future[Unit]] = new JHMap

  def asyncWriteMessages(messages: immutable.Seq[AtomicWrite]): Future[immutable.Seq[Try[Unit]]] = {
    val serializedTries: immutable.Seq[Try[immutable.Seq[JournalEntry]]] = messages.map { atomicWrite =>
      // Since all PersistentRepr are persisted atomically they all get the same timestamp
      val now = System.currentTimeMillis()
      val serialized = atomicWrite.payload.map(pr => serializer.serialize(pr.withTimestamp(now)))
      TrySeq.flatten(serialized)
    }

    // If serialization fails for some AtomicWrites, the other AtomicWrites may still be written
    val entriesToWrite: immutable.Seq[JournalEntry] = for {
      serializeTry: Try[immutable.Seq[JournalEntry]] <- serializedTries
      row: JournalEntry <- serializeTry.getOrElse(immutable.Seq.empty)
    } yield row

    val pid = messages.head.persistenceId

    val future: Future[Unit] = Source(List(entriesToWrite))
        .flatMapConcat(events => dao.writeEvents(events))
        .map(_ => ())
        .runWith(Sink.last[Unit])

    writeInProgress.put(pid, future)
    future.map(_ => TrySeq.writeCompleteSignal(serializedTries))
        .andThen{case _ => writeInProgress.remove(pid)}
  }

  def asyncDeleteMessagesTo(persistenceId: String, toSequenceNr: Long): Future[Unit] = {
    dao.deleteEvents(persistenceId, toSequenceNr)
        .map(_ => ())
        .runWith(Sink.last) // Wait for the transaction commit and the connection to be closed
  }

  def asyncReplayMessages(
      persistenceId: String,
      fromSequenceNr: Long,
      toSequenceNr: Long,
      max: Long
  )(recoveryCallback: PersistentRepr => Unit): Future[Unit] = {
    // TODO: Protect the fetchEvents call with a circuit-breaker
    dao.fetchEvents(persistenceId, fromSequenceNr, toSequenceNr, max)
        .mapAsync(1)(event => Future.fromTry(serializer.deserialize(event)))
        .runForeach(repr => recoveryCallback(repr))
        .map(_ => ())
  }

  def asyncReadHighestSequenceNr(persistenceId: String, fromSequenceNr: Long): Future[Long] = {
    def go(persistenceId: String, fromSequenceNr: Long) =
      dao.readHighestSequenceNr(persistenceId, fromSequenceNr)
          .orElse(Source.single(0L))
          .runWith(Sink.last)

    writeInProgress.get(persistenceId) match {
      case null =>
        go(persistenceId, fromSequenceNr)
      case f =>
        f.flatMap(_ => go(persistenceId, fromSequenceNr))
    }
  }

}
