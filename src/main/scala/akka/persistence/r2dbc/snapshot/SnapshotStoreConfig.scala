package akka.persistence.r2dbc.snapshot

import com.typesafe.config.Config

object SnapshotStoreConfig {

  def apply(config: Config): SnapshotStoreConfig = new SnapshotStoreConfig(config)
}

final class SnapshotStoreConfig(config: Config) {
  val hostname: String = config.getString("db.hostname")
  val username: String = config.getString("db.username")
  val password: String = config.getString("db.password")
  val database: String = config.getString("db.database")
}
