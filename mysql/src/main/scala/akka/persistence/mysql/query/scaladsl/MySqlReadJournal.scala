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

package akka.persistence.mysql.query.scaladsl

import akka.actor.ExtendedActorSystem
import akka.persistence.r2dbc.ConnectionPoolFactory
import akka.persistence.r2dbc.client.R2dbc
import akka.persistence.r2dbc.query.{QueryDao, ReactiveReadJournal, ReadJournalConfig}
import com.typesafe.config.Config

private[akka] object MySqlReadJournal {

  /**
   * The default identifier for [[MySqlReadJournal]] to be used with
   * `akka.persistence.query.PersistenceQuery#readJournalFor`.
   *
   * The value is `"mysql-read-journal"` and corresponds
   * to the absolute path to the read journal configuration entry.
   */
  final val Identifier = "mysql-read-journal"

}

/**
 * Scala API `akka.persistence.query.scaladsl.ReadJournal` implementation for MySQL
 * with R2DBC a driver.
 *
 * It is retrieved with:
 * {{{
 * val readJournal = PersistenceQuery(system).readJournalFor[MySqlReadJournal](MySqlReadJournal.Identifier)
 * }}}
 *
 * Corresponding Java API is in [[akka.persistence.mysql.query.javadsl.MySqlReadJournal]].
 *
 * Configuration settings can be defined in the configuration section with the
 * absolute path corresponding to the identifier, which is `"mysql-read-journal"`
 * for the default [[MySqlReadJournal#Identifier]]. See `reference.conf`.
 */
private[query] final class MySqlReadJournal(val system: ExtendedActorSystem, config: Config)
    extends ReactiveReadJournal {

  override protected val dao: QueryDao =
    new MySqlQueryDao(R2dbc(ConnectionPoolFactory("mysql", ReadJournalConfig(system, config))))

}
