package akka.persistence.r2dbc.journal

import akka.persistence.r2dbc.{DatabaseConfig, PluginConfig}
import com.typesafe.config.Config

private[akka] object JournalPluginConfig {
  def apply(config: Config): JournalPluginConfig = new JournalPluginConfig(config)
}
private[akka] final class JournalPluginConfig(config: Config) extends PluginConfig {
  override val db: DatabaseConfig = DatabaseConfig(config.getConfig("db"), config.getConfig("db.pool"))
}
