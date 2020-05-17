package akka.persistence.r2dbc.journal

import akka.actor.ActorSystem
import akka.persistence.{AtomicWrite, PersistentRepr}
import akka.serialization.SerializationExtension
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import java.util.{HashMap => JHMap, Map => JMap}
import scala.collection.immutable.Seq
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.Try

/**
 * A mixin with the journal logic (separated from [[ReactiveJournal]] to make testing easier).
 */
trait JournalLogic {

  implicit val system: ActorSystem
  lazy val serializer = new PersistenceReprSerializer(SerializationExtension(system))

  private lazy implicit val mat: Materializer = Materializer(system)
  private lazy implicit val ec: ExecutionContextExecutor = system.dispatcher
  protected val dao: JournalDao
  private val writeInProgress: JMap[String, Future[Unit]] = new JHMap

  def asyncWriteMessages(messages: Seq[AtomicWrite]): Future[Seq[Try[Unit]]] = {
    val serializedTries: Seq[Try[Seq[JournalEntry]]] = messages.map { atomicWrite =>
      val serialized = atomicWrite.payload.map(serializer.serialize)
      TrySeq.flatten(serialized)
    }

    // If serialization fails for some AtomicWrites, the other AtomicWrites may still be written
    val entriesToWrite: Seq[JournalEntry] = for {
      serializeTry: Try[Seq[JournalEntry]] <- serializedTries
      row: JournalEntry <- serializeTry.getOrElse(Seq.empty)
    } yield row

    val pid = messages.head.persistenceId

    val future = Source(List(entriesToWrite))
        .flatMapConcat(events => dao.writeEvents(events) )
        .map(_ => ())
        .runWith(Sink.last[Unit])

    writeInProgress.put(pid, future)
    future.map(_ => TrySeq.writeCompleteSignal(serializedTries))
        .andThen(_ => writeInProgress.remove(pid))
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
