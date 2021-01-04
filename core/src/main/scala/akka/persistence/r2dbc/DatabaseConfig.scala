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
