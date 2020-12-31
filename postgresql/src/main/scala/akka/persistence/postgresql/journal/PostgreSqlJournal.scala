/*
 * Copyright 2020 Borislav Borisov.
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

package akka.persistence.postgresql.journal

import akka.actor.ActorSystem
import akka.persistence.r2dbc.ConnectionPoolFactory
import akka.persistence.r2dbc.client.R2dbc
import akka.persistence.r2dbc.journal.{JournalDao, JournalPluginConfig, ReactiveJournal}
import com.typesafe.config.Config

private[akka] final class PostgreSqlJournal(config: Config)
    extends ReactiveJournal {

  override implicit val system: ActorSystem = context.system
  override protected val dao: JournalDao =
    new PostgreSqlJournalDao(R2dbc(ConnectionPoolFactory("postgresql", JournalPluginConfig(config))))

}
