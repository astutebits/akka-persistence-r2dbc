package akka.persistence.r2dbc.query

import akka.actor.ExtendedActorSystem
import akka.persistence.r2dbc.{DatabaseConfig, PluginConfig}
import com.typesafe.config.Config
import java.time.{Duration => JDuration}

private[akka] object ReadJournalConfig {
  def apply(system: ExtendedActorSystem, config: Config): ReadJournalConfig =
    new ReadJournalConfig(system, config)
}

private[akka] final class ReadJournalConfig(system: ExtendedActorSystem, config: Config) extends PluginConfig {

  override val db: DatabaseConfig = DatabaseConfig(
    system.settings.config.getConfig(config.getString("journal-plugin")).getConfig("db"),
    config.getConfig("db.pool")
  )

  val refreshInterval: JDuration = config.getDuration("refresh-interval")

}
