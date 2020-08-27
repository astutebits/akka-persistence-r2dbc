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

package akka.persistence.postgresql.query.scaladsl

import akka.actor.ExtendedActorSystem
import akka.persistence.r2dbc.ConnectionPoolFactory
import akka.persistence.r2dbc.client.R2dbc
import akka.persistence.r2dbc.query.{QueryDao, ReactiveReadJournal, ReadJournalConfig}
import com.typesafe.config.Config

object PostgreSqlReadJournal {

  /**
   * The default identifier for [[PostgreSqlReadJournal]] to be used with
   * `akka.persistence.query.PersistenceQuery#readJournalFor`.
   *
   * The value is `"postgresql-read-journal"` and corresponds
   * to the absolute path to the read journal configuration entry.
   */
  final val Identifier = "postgresql-read-journal"

}

/**
 * Scala API `akka.persistence.query.scaladsl.ReadJournal` implementation for PostgreSQL
 * with R2DBC a driver.
 *
 * It is retrieved with:
 * {{{
 * val readJournal = PersistenceQuery(system).readJournalFor[PostgreSqlReadJournal](PostgreSqlReadJournal.Identifier)
 * }}}
 *
 * Corresponding Java API is in [[akka.persistence.postgresql.query.javadsl.PostgreSqlReadJournal]].
 *
 * Configuration settings can be defined in the configuration section with the
 * absolute path corresponding to the identifier, which is `"postgresql-read-journal"`
 * for the default [[PostgreSqlReadJournal#Identifier]]. See `reference.conf`.
 */
final class PostgreSqlReadJournal(val system: ExtendedActorSystem, config: Config)
    extends ReactiveReadJournal {

  override protected val dao: QueryDao =
    new PostgreSqlQueryDao(new R2dbc(ConnectionPoolFactory("postgresql", ReadJournalConfig(system, config))))

}
