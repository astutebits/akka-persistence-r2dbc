package akka.persistence.postgresql.journal

import akka.actor.ActorSystem
import akka.persistence.r2dbc.client.R2dbc
import akka.persistence.r2dbc.journal.{JournalDao, ReactiveJournal}
import com.typesafe.config.Config
import io.r2dbc.postgresql.{PostgresqlConnectionConfiguration, PostgresqlConnectionFactory}

class PostgresqlJournal(config: Config)
    extends ReactiveJournal {

  private val pluginConfig = PostgresqlPluginConfig(config)
  private val factory = new PostgresqlConnectionFactory(
    PostgresqlConnectionConfiguration.builder()
        .host(pluginConfig.hostname)
        .username(pluginConfig.username)
        .password(pluginConfig.password)
        .database(pluginConfig.database)
        .build())

  override implicit val system: ActorSystem = context.system
  override protected val dao: JournalDao = new PostgresqlJournalDao(new R2dbc(factory))

}
