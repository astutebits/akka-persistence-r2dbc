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

package akka.persistence.r2dbc.snapshot

import akka.NotUsed
import akka.persistence.SnapshotSelectionCriteria
import akka.stream.scaladsl.Source

private[akka] trait SnapshotStoreDao {

  /**
   * Returns a single [[SnapshotEntry]] in the [[Source]] that matches the criteria for the given
   * `persistenceId`.
   *
   * @param persistenceId the persistence id.
   * @param criteria      selection criteria.
   * @return a single entry [[Source]] with the [[SnapshotEntry]], or empty if not found.
   */
  def fetchSnapshot(persistenceId: String, criteria: SnapshotSelectionCriteria): Source[SnapshotEntry, NotUsed]

  /**
   * Persists the snapshot entry and returns a [[Source]] with one [[Int]] element with the number
   * of rows that were added.
   *
   * @param entry the snapshot entry.
   * @return a [[Source]] with one [[Int]] element with the number of rows that were added.
   */
  def save(entry: SnapshotEntry): Source[Int, NotUsed]

  /**
   * Deletes all snapshots after the sequence_nr for the given persistence id.
   *
   * @param persistenceId the persistence id.
   * @param seqNr         sequence_nr (inclusive)
   * @return a [[Source]] with one [[Int]] element with the number of rows that were added.
   */
  def deleteSnapshot(persistenceId: String, seqNr: Long): Source[Int, NotUsed]

  /**
   * Delete all snapshots for the given persistence id that match the selection criteria.
   *
   * @param persistenceId the persistence id.
   * @param criteria      selection criteria.
   * @return a [[Source]] with one [[Int]] element with the number of rows that were added.
   */
  def deleteSnapshot(persistenceId: String, criteria: SnapshotSelectionCriteria): Source[Int, NotUsed]

}
