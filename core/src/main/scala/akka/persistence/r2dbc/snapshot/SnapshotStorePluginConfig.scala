package akka.persistence.r2dbc.snapshot

import akka.persistence.r2dbc.{DatabaseConfig, PluginConfig}
import com.typesafe.config.Config

private[akka] object SnapshotStorePluginConfig {
  def apply(config: Config): SnapshotStorePluginConfig = new SnapshotStorePluginConfig(config)
}
private[akka] final class SnapshotStorePluginConfig(config: Config) extends PluginConfig {
  override val db: DatabaseConfig = DatabaseConfig(config.getConfig("db"), config.getConfig("db.pool"))
}
