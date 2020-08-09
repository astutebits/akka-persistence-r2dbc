package akka.persistence.r2dbc

import io.r2dbc.pool.{ConnectionPool, ConnectionPoolConfiguration}
import io.r2dbc.spi.{ConnectionFactories, ConnectionFactory, ConnectionFactoryOptions}

private[akka] trait PluginConfig {
  val db: DatabaseConfig
}

private[akka] object ConnectionPoolFactory extends ((String, PluginConfig) => ConnectionFactory) {
  private val validDrivers = Set("mysql", "postgresql")

  def apply(driver: String, pluginConfig: PluginConfig): ConnectionFactory = {
    if (!validDrivers.contains(driver))
      throw new IllegalArgumentException(s"Driver '$driver' not supported.")
    val factoryOptions = ConnectionFactoryOptions.builder()
        .option(ConnectionFactoryOptions.DRIVER, driver)
        .option(ConnectionFactoryOptions.PROTOCOL, driver)
        .option(ConnectionFactoryOptions.HOST, pluginConfig.db.hostname)
        .option(ConnectionFactoryOptions.PORT, Integer.valueOf(pluginConfig.db.port))
        .option(ConnectionFactoryOptions.USER, pluginConfig.db.username)
        .option(ConnectionFactoryOptions.PASSWORD, pluginConfig.db.password)
        .option(ConnectionFactoryOptions.DATABASE, pluginConfig.db.database)
        .build()
    new ConnectionPool(
      ConnectionPoolConfiguration.builder(ConnectionFactories.get(factoryOptions))
          .initialSize(pluginConfig.db.poolInitialSize)
          .maxSize(pluginConfig.db.poolMaxSize)
          .maxLifeTime(pluginConfig.db.poolMaxLifeTime)
          .maxIdleTime(pluginConfig.db.poolMaxIdleTime)
          .maxAcquireTime(pluginConfig.db.poolMaxAcquireTime)
          .maxCreateConnectionTime(pluginConfig.db.poolMaxCreateConnectionTime)
          .build()
    )
  }
}
