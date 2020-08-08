package akka.persistence.postgresql.journal

import akka.actor.ActorSystem
import akka.persistence.r2dbc.ConnectionPoolFactory
import akka.persistence.r2dbc.client.R2dbc
import akka.persistence.r2dbc.journal.{JournalDao, JournalPluginConfig, ReactiveJournal}
import com.typesafe.config.Config

private[akka] final class PostgreSqlJournal(config: Config)
    extends ReactiveJournal {

  override implicit val system: ActorSystem = context.system
  override protected val dao: JournalDao =
    new PostgreSqlJournalDao(new R2dbc(ConnectionPoolFactory("postgresql", JournalPluginConfig(config))))

}
