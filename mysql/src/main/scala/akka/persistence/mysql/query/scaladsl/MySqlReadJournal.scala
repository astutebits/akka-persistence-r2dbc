package akka.persistence.mysql.query.scaladsl

import akka.actor.ExtendedActorSystem
import akka.persistence.r2dbc.ConnectionPoolFactory
import akka.persistence.r2dbc.client.R2dbc
import akka.persistence.r2dbc.query.{QueryDao, ReactiveReadJournal, ReadJournalConfig}
import com.typesafe.config.Config

private[akka] object MySqlReadJournal {

  /**
   * The default identifier for [[MySqlReadJournal]] to be used with
   * `akka.persistence.query.PersistenceQuery#readJournalFor`.
   *
   * The value is `"mysql-read-journal"` and corresponds
   * to the absolute path to the read journal configuration entry.
   */
  final val Identifier = "mysql-read-journal"

}

/**
 * Scala API `akka.persistence.query.scaladsl.ReadJournal` implementation for MySQL
 * with R2DBC a driver.
 *
 * It is retrieved with:
 * {{{
 * val readJournal = PersistenceQuery(system).readJournalFor[MySqlReadJournal](MySqlReadJournal.Identifier)
 * }}}
 *
 * Corresponding Java API is in [[akka.persistence.mysql.query.javadsl.MySqlReadJournal]].
 *
 * Configuration settings can be defined in the configuration section with the
 * absolute path corresponding to the identifier, which is `"mysql-read-journal"`
 * for the default [[MySqlReadJournal#Identifier]]. See `reference.conf`.
 */
private[query] final class MySqlReadJournal(val system: ExtendedActorSystem, config: Config)
    extends ReactiveReadJournal {

  override protected val dao: QueryDao =
    new MySqlQueryDao(new R2dbc(ConnectionPoolFactory("mysql", ReadJournalConfig(system, config))))

}
