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

package akka.persistence.postgresql.query

import akka.actor.ExtendedActorSystem
import akka.persistence.query.ReadJournalProvider
import com.typesafe.config.Config

private[akka] final class PostgresqlReadJournalProvider(system: ExtendedActorSystem, config: Config)
    extends ReadJournalProvider {

  override val scaladslReadJournal: scaladsl.PostgreSqlReadJournal =
    new scaladsl.PostgreSqlReadJournal(system, config)

  override val javadslReadJournal: javadsl.PostgreSqlReadJournal =
    new javadsl.PostgreSqlReadJournal(scaladslReadJournal)

}
