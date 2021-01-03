/*
 * Copyright 2020-2021 Borislav Borisov.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
