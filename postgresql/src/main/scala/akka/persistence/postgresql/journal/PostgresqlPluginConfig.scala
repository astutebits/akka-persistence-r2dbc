package akka.persistence.postgresql.journal

import com.typesafe.config.Config

object PostgresqlPluginConfig {
  def apply(config: Config): PostgresqlPluginConfig = new PostgresqlPluginConfig(config)
}
final class PostgresqlPluginConfig(config: Config) {
  val hostname: String = config.getString("db.hostname")
  val username: String = config.getString("db.username")
  val password: String = config.getString("db.password")
  val database: String = config.getString("db.database")
}
