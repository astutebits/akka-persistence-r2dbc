package akka.persistence.mysql.journal

import akka.actor.ActorSystem
import akka.persistence.r2dbc.ConnectionPoolFactory
import akka.persistence.r2dbc.client.R2dbc
import akka.persistence.r2dbc.journal.{JournalPluginConfig, ReactiveJournal}
import com.typesafe.config.Config

/**
 * An implementation of the `Journal plugin API` with `r2dbc-mysql`.
 *
 * @see [[https://doc.akka.io/docs/akka/current/persistence-journals.html#journal-plugin-api Journal plugin API]]
 * @see [[https://github.com/mirromutth/r2dbc-mysql r2dbc-mysql]]
 */
final class MySqlJournal(config: Config)
    extends ReactiveJournal {

  override implicit val system: ActorSystem = context.system
  override protected val dao = new MySqlJournalDao(new R2dbc(ConnectionPoolFactory("mysql", JournalPluginConfig(config))))

}
