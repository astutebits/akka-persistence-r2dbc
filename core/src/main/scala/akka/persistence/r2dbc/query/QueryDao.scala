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

import akka.NotUsed
import akka.persistence.r2dbc.journal.JournalEntry
import akka.stream.scaladsl.Source

private[akka] trait QueryDao {

  /**
   * Returns a tuple of distinct persistence ids and the max index found for each persistence id
   * (ordered by the index).
   *
   * A call similar to the one below:
   * {{{
   * SELECT persistence_id, max(index) AS index FROM journal_event WHERE index > 50000 GROUP BY persistence_id ORDER BY index;
   * }}}
   *
   * @param offset from index (inclusive)
   * @return a [[Source]] with tuple of `persistenceId`s and max `index`es.
   */
  def fetchPersistenceIds(offset: Long): Source[(Long, String), NotUsed]

  /**
   * Returns a [[Source]] with a subset of [[JournalEntry]] in the selected `sequenceNr` range for
   * the given `persistenceId`.
   *
   * @param persistenceId the persistence id.
   * @param fromSeqNr from sequence number (inclusive).
   * @param toSeqNr to sequence number (inclusive).
   * @return a [[Source]] with [[JournalEntry]]s matching the criteria.
   */
  def fetchByPersistenceId(persistenceId: String, fromSeqNr: Long, toSeqNr: Long): Source[JournalEntry, NotUsed]

  /**
   * Returns a [[Source]] with a subset of [[JournalEntry]] with record `index`es in the selected
   * range for the given `tag`.
   *
   * @param tag the tag.
   * @param fromIndex from index (inclusive).
   * @param toIndex to index (inclusive).
   * @return a [[Source]] with [[JournalEntry]]s matching the criteria.
   */
  def fetchByTag(tag: String, fromIndex: Long, toIndex: Long): Source[JournalEntry, NotUsed]

  /**
   * Returns a single element [[Source]] with the highest record index for the given `tag`, or 0 if
   * a record is not found.
   *
   * @param tag the tag.
   * @return a [[Source]] with a single [[Long]] element.
   */
  def findHighestIndex(tag: String): Source[Long, NotUsed]

  /**
   * Returns a single element [[Source]] with the highest `sequenceNr` for the given `persistenceId`,
   * or 0 if a record is not found.
   *
   * @param persistenceId the persistence id.
   * @return a [[Source]] with a single [[Long]] element.
   */
  def findHighestSeq(persistenceId: String): Source[Long, NotUsed]

}
