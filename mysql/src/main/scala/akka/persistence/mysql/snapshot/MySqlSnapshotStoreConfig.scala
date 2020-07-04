package akka.persistence.mysql.snapshot

import com.typesafe.config.Config

private[akka] object MySqlSnapshotStoreConfig {
  def apply(config: Config): MySqlSnapshotStoreConfig = new MySqlSnapshotStoreConfig(config)
}

private[akka] final class MySqlSnapshotStoreConfig(config: Config) {
  val hostname: String = config.getString("db.hostname")
  val username: String = config.getString("db.username")
  val password: String = config.getString("db.password")
  val database: String = config.getString("db.database")
}
