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

package akka.persistence.mysql.query.javadsl

import akka.NotUsed
import akka.persistence.mysql.query.scaladsl.{MySqlReadJournal => ScalaMySqlReadJournal}
import akka.persistence.query.javadsl._
import akka.persistence.query.{EventEnvelope, Offset}
import akka.stream.javadsl.Source

private[akka] object MySqlReadJournal {

  /**
   * The default identifier for [[MySqlReadJournal]] to be used with
   * `akka.persistence.query.PersistenceQuery#getReadJournalFor`.
   *
   * The value is `"mysql-read-journal"` and corresponds
   * to the absolute path to the read journal configuration entry.
   */
  final val Identifier = ScalaMySqlReadJournal.Identifier

}

/**
 * Java API: `akka.persistence.query.javadsl.ReadJournal` implementation for MySQL
 * with R2DBC a driver.
 *
 * It is retrieved with:
 * {{{
 * MySqlReadJournal queries =
 *   PersistenceQuery.get(system).getReadJournalFor(MySqlReadJournal.class, MySqlReadJournal.Identifier());
 * }}}
 *
 * Corresponding Scala API is in [[akka.persistence.mysql.query.scaladsl.MySqlReadJournal]].
 *
 * Configuration settings can be defined in the configuration section with the
 * absolute path corresponding to the identifier, which is `"mysql-read-journal"`
 * for the default [[MySqlReadJournal#Identifier]]. See `reference.conf`.
 */
private[query] final class MySqlReadJournal(readJournal: ScalaMySqlReadJournal)
    extends ReadJournal
        with CurrentPersistenceIdsQuery
        with PersistenceIdsQuery
        with CurrentEventsByPersistenceIdQuery
        with EventsByPersistenceIdQuery
        with CurrentEventsByTagQuery
        with EventsByTagQuery {

  override def currentPersistenceIds(): Source[String, NotUsed] =
    readJournal.currentPersistenceIds().asJava

  override def persistenceIds(): Source[String, NotUsed] =
    readJournal.persistenceIds().asJava

  override def currentEventsByPersistenceId(
      persistenceId: String,
      fromSequenceNr: Long,
      toSequenceNr: Long
  ): Source[EventEnvelope, NotUsed] =
    readJournal.currentEventsByPersistenceId(persistenceId, fromSequenceNr, toSequenceNr).asJava

  override def eventsByPersistenceId(
      persistenceId: String,
      fromSequenceNr: Long,
      toSequenceNr: Long
  ): Source[EventEnvelope, NotUsed] =
    readJournal.eventsByPersistenceId(persistenceId, fromSequenceNr, toSequenceNr).asJava

  override def currentEventsByTag(tag: String, offset: Offset): Source[EventEnvelope, NotUsed] =
    readJournal.currentEventsByTag(tag, offset).asJava

  override def eventsByTag(tag: String, offset: Offset): Source[EventEnvelope, NotUsed] =
    readJournal.eventsByTag(tag, offset).asJava

}
