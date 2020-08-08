package akka.persistence.r2dbc.snapshot

import akka.persistence.r2dbc.journal.JournalPluginConfig
import com.typesafe.config.Config

private[akka] object SnapshotStorePluginConfig {
  def apply(config: Config): SnapshotStorePluginConfig = new SnapshotStorePluginConfig(config)
}
private[akka] final class SnapshotStorePluginConfig(config: Config) extends JournalPluginConfig(config) {

}
