package akka.persistence.postgresql.query.scaladsl

import akka.actor.ExtendedActorSystem
import akka.persistence.postgresql.journal.PostgresqlPluginConfig
import akka.persistence.r2dbc.client.R2dbc
import akka.persistence.r2dbc.query.{QueryDao, ReactiveReadJournal}
import com.typesafe.config.Config
import io.r2dbc.postgresql.{PostgresqlConnectionConfiguration, PostgresqlConnectionFactory}

object PostgresqlReadJournal {

  /**
   * The default identifier for [[PostgresqlReadJournal]] to be used with
   * `akka.persistence.query.PersistenceQuery#readJournalFor`.
   *
   * The value is `"postgresql-read-journal"` and corresponds
   * to the absolute path to the read journal configuration entry.
   */
  final val Identifier = "postgresql-read-journal"

}

/**
 * Scala API `akka.persistence.query.scaladsl.ReadJournal` implementation for PostgreSQL
 * with R2DBC a driver.
 *
 * It is retrieved with:
 * {{{
 * val readJournal = PersistenceQuery(system).readJournalFor[PostgresqlReadJournal](PostgresqlReadJournal.Identifier)
 * }}}
 *
 * Corresponding Java API is in [[akka.persistence.postgresql.query.javadsl.PostgresqlReadJournal]].
 *
 * Configuration settings can be defined in the configuration section with the
 * absolute path corresponding to the identifier, which is `"postgresql-read-journal"`
 * for the default [[PostgresqlReadJournal#Identifier]]. See `reference.conf`.
 */
final class PostgresqlReadJournal(val system: ExtendedActorSystem, config: Config)
    extends ReactiveReadJournal {

  private val pluginConfig = PostgresqlPluginConfig(system.settings.config.getConfig(config.getString("journal-plugin")))
  private val factory = new PostgresqlConnectionFactory(
    PostgresqlConnectionConfiguration.builder()
        .host(pluginConfig.hostname)
        .username(pluginConfig.username)
        .password(pluginConfig.password)
        .database(pluginConfig.database)
        .build())

  override protected val dao: QueryDao = new PostgresqlQueryDao(new R2dbc(factory))

}
