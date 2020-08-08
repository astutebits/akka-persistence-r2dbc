package akka.persistence.r2dbc.journal

import com.typesafe.config.Config
import java.time.{Duration => JDuration}

private[akka] object JournalPluginConfig {
  def apply(config: Config): JournalPluginConfig = new JournalPluginConfig(config)
}
private[akka] class JournalPluginConfig(config: Config) {
  val hostname: String = config.getString("db.hostname")
  val port: Int = config.getInt("db.port")
  val username: String = config.getString("db.username")
  val password: String = config.getString("db.password")
  val database: String = config.getString("db.database")

  val poolInitialSize: Int = config.getInt("db.pool.initial-size")
  val poolMaxSize: Int = config.getInt("db.pool.max-size")
  val poolMaxLifeTime: JDuration = config.getDuration("db.pool.max-life-time")
  val poolMaxIdleTime: JDuration = config.getDuration("db.pool.max-idle-time")
  val poolMaxAcquireTime: JDuration = config.getDuration("db.pool.max-acquire-time")
  val poolMaxCreateConnectionTime: JDuration = config.getDuration("db.pool.max-create-connection-time")
}
