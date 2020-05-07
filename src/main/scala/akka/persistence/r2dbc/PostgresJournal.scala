package akka.persistence.r2dbc

import akka.persistence.{AtomicWrite, PersistentRepr}
import akka.persistence.journal._
import com.typesafe.config.Config
import scala.concurrent.Future
import scala.util.Try


class PostgresJournal(config: Config) extends AsyncWriteJournal {
  override def asyncWriteMessages(messages: Seq[AtomicWrite]): Future[Seq[Try[Unit]]] = ???

  override def asyncDeleteMessagesTo(persistenceId: String, toSequenceNr: Long): Future[Unit] = ???

  override def asyncReplayMessages(persistenceId: String, fromSequenceNr: Long, toSequenceNr: Long, max: Long)(recoveryCallback: PersistentRepr => Unit): Future[Unit] = ???

  override def asyncReadHighestSequenceNr(persistenceId: String, fromSequenceNr: Long): Future[Long] = ???
}
