package akka.persistence.mysql.journal

import com.typesafe.config.Config


final class MySqlPluginConfig(config: Config) {
  val hostname: String = config.getString("db.hostname")
  val username: String = config.getString("db.username")
  val password: String = config.getString("db.password")
  val database: String = config.getString("db.database")
}
