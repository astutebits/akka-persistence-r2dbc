package akka.persistence.r2dbc

import com.typesafe.config.Config

object PluginConfig {
  def apply(config: Config): PluginConfig = new PluginConfig(config)
}
final class PluginConfig(config: Config) {
  val hostname: String = config.getString("db.hostname")
  val username: String = config.getString("db.username")
  val password: String = config.getString("db.password")
  val database: String = config.getString("db.database")

}
