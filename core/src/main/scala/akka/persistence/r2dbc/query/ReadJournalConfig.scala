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

package akka.persistence.r2dbc.query

import akka.actor.ExtendedActorSystem
import akka.persistence.r2dbc.{DatabaseConfig, PluginConfig}
import com.typesafe.config.Config
import java.time.{Duration => JDuration}

private[akka] object ReadJournalConfig {
  def apply(system: ExtendedActorSystem, config: Config): ReadJournalConfig =
    new ReadJournalConfig(system, config)
}

private[akka] final class ReadJournalConfig(system: ExtendedActorSystem, config: Config) extends PluginConfig {

  override val db: DatabaseConfig = DatabaseConfig(
    system.settings.config.getConfig(config.getString("journal-plugin")).getConfig("db"),
    config.getConfig("db.pool")
  )

  val refreshInterval: JDuration = config.getDuration("refresh-interval")

}
