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

package akka.persistence.postgresql.snapshot

import akka.persistence.SnapshotSelectionCriteria
import akka.persistence.r2dbc.snapshot.SnapshotEntry
import io.netty.buffer.ByteBufUtil

private[akka] object PostgreSqlSnapshotStoreQueries {

  // Note, the two criterion - minSeqNr and minTs - do not appear to be used anywhere. But we've
  // added them in case they are used in the future.
  def fetchSnapshotQuery(persistenceId: String, criteria: SnapshotSelectionCriteria): String =
    "SELECT persistence_id, sequence_nr, instant, snapshot FROM snapshot " +
        "WHERE persistence_id = '" + persistenceId + "'" + selectionCriteria(criteria) +
        " ORDER BY sequence_nr DESC LIMIT 1"

  def deleteSnapshotQuery(persistenceId: String, seqNr: Long): String =
    "DELETE FROM snapshot WHERE persistence_id = '" + persistenceId + "' " + "AND sequence_nr = " + seqNr

  def deleteSnapshotQuery(persistenceId: String, criteria: SnapshotSelectionCriteria): String =
    "DELETE FROM snapshot WHERE persistence_id = '" + persistenceId + "'" + selectionCriteria(criteria)

  private def selectionCriteria(criteria: SnapshotSelectionCriteria) = {
    var query = ""
    if (criteria.maxSequenceNr != Long.MaxValue) query += " AND sequence_nr <= " + criteria.maxSequenceNr
    if (criteria.maxTimestamp != Long.MaxValue) query += " AND instant <= " + criteria.maxTimestamp
    if (criteria.minSequenceNr != 0L) query += " AND sequence_nr => " + criteria.minSequenceNr
    if (criteria.minTimestamp != 0L) query += " AND instant => " + criteria.minTimestamp
    query
  }

  def upsertSnapshotQuery(entry: SnapshotEntry): String = {
    val snapshotHex = "\\x" + ByteBufUtil.hexDump(entry.snapshot)
    "INSERT INTO snapshot (persistence_id, sequence_nr, instant, snapshot) VALUES " +
        s"('${entry.persistenceId}', ${entry.sequenceNr}, ${entry.instant}, '$snapshotHex')" +
        s" ON CONFLICT (persistence_id, sequence_nr) DO UPDATE SET instant = ${entry.instant}, snapshot = '$snapshotHex'"
  }
}
