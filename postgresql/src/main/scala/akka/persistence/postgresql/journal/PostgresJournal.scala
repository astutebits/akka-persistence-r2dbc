package akka.persistence.postgresql.journal

import akka.Done
import akka.persistence._
import akka.persistence.journal._
import akka.serialization.SerializationExtension
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import com.typesafe.config.Config
import io.r2dbc.postgresql.{PostgresqlConnectionConfiguration, PostgresqlConnectionFactory}
import java.util.{HashMap => JHMap, Map => JMap}
import scala.collection.immutable.Seq
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.jdk.CollectionConverters._
import scala.util.Try


private[akka] object PostgresJournal {

  final case class WriteFinished(pid: String, f: Future[_])

}

class PostgresJournal(config: Config) extends AsyncWriteJournal {

  import PostgresJournal._

  private implicit val mat: Materializer = Materializer(context.system)
  private implicit val ec: ExecutionContext = context.dispatcher

  private val writeInProgress: JMap[String, Future[Done]] = new JHMap
  private val serializer = new PersistenceReprSerializer(SerializationExtension(context.system))
  private val pluginConfig = PostgresqlPluginConfig(config)

  private val cf = new PostgresqlConnectionFactory(PostgresqlConnectionConfiguration.builder()
      .host(pluginConfig.hostname)
      .username(pluginConfig.username)
      .password(pluginConfig.password)
      .database(pluginConfig.database)
      .build())

  private val dao = new PostgresJournalDao(cf)

  override def asyncWriteMessages(messages: Seq[AtomicWrite]): Future[Seq[Try[Unit]]] = {
    val serializedTries: Seq[Try[Seq[JournalEntry]]] = messages.map { atomicWrite =>
      val serialized = atomicWrite.payload.map(serializer.serialize)
      TrySeq.flatten(serialized)
    }

    // If serialization fails for some AtomicWrites, the other AtomicWrites may still be written
    val entriesToWrite: Seq[JournalEntry] = for {
      serializeTry: Try[Seq[JournalEntry]] <- serializedTries
      row: JournalEntry <- serializeTry.getOrElse(Seq.empty)
    } yield row

    val promise = Promise[Done]
    val pid = messages.head.persistenceId
    writeInProgress.put(pid, promise.future)

    Source.fromPublisher[Integer](dao.writeEvents(entriesToWrite.asJava, promise))
        .runWith(Sink.ignore)

    promise.future
        .map(_ => TrySeq.writeCompleteSignal(serializedTries))
        .andThen(_ => self ! WriteFinished(pid, promise.future))
  }

  override def asyncDeleteMessagesTo(persistenceId: String, toSequenceNr: Long): Future[Unit] = {
    Source.fromPublisher(dao.deleteEvents(persistenceId, toSequenceNr))
        .map(_ => ())
        .runWith(Sink.last) // Wait for the transaction commit and the connection to be closed
  }

  override def asyncReplayMessages(
      persistenceId: String,
      fromSequenceNr: Long,
      toSequenceNr: Long,
      max: Long
  )(recoveryCallback: PersistentRepr => Unit): Future[Unit] = {
    Source.fromPublisher(dao.fetchEvents(persistenceId, fromSequenceNr, toSequenceNr, max))
        .mapAsync(1)(event => Future.fromTry(serializer.deserialize(event)))
        .runForeach(repr => recoveryCallback(repr))
        .map(_ => ())
  }

  override def asyncReadHighestSequenceNr(persistenceId: String, fromSequenceNr: Long): Future[Long] = {
    def go(persistenceId: String, fromSequenceNr: Long) =
      Source.fromPublisher(dao.readHighestSequenceNr(persistenceId, fromSequenceNr))
          .map(_.toLong)
          .runWith(Sink.last)

    writeInProgress.get(persistenceId) match {
      case null => go(persistenceId, fromSequenceNr)
      case f => f.flatMap(_ => go(persistenceId, fromSequenceNr))
    }
  }

  override def receivePluginInternal: Receive = {
    case WriteFinished(persistenceId, future) =>
      writeInProgress.remove(persistenceId, future)
  }

}
