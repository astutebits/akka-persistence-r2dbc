package akka.persistence.postgresql.journal

import com.typesafe.config.Config

private[akka] object PostgreSqlPluginConfig {
  def apply(config: Config): PostgreSqlPluginConfig = new PostgreSqlPluginConfig(config)
}
final class PostgreSqlPluginConfig(config: Config) {
  val hostname: String = config.getString("db.hostname")
  val username: String = config.getString("db.username")
  val password: String = config.getString("db.password")
  val database: String = config.getString("db.database")
}
