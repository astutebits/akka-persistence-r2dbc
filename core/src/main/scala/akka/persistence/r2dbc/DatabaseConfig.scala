package akka.persistence.r2dbc

import com.typesafe.config.Config
import java.time.{Duration => JDuration}

private[akka] case class DatabaseConfig(connection: Config, pool: Config) {
  val hostname: String = connection.getString("hostname")
  val port: Int = connection.getInt("port")
  val username: String = connection.getString("username")
  val password: String = connection.getString("password")
  val database: String = connection.getString("database")

  val poolInitialSize: Int = pool.getInt("initial-size")
  val poolMaxSize: Int = pool.getInt("max-size")
  val poolMaxLifeTime: JDuration = pool.getDuration("max-life-time")
  val poolMaxIdleTime: JDuration = pool.getDuration("max-idle-time")
  val poolMaxAcquireTime: JDuration = pool.getDuration("max-acquire-time")
  val poolMaxCreateConnectionTime: JDuration = pool.getDuration("max-create-connection-time")
}
