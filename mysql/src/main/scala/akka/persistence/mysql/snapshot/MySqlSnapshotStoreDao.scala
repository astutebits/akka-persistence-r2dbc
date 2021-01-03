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

package akka.persistence.mysql.snapshot

import akka.NotUsed
import akka.persistence.SnapshotSelectionCriteria
import akka.persistence.r2dbc.client.R2dbc
import akka.persistence.r2dbc.snapshot.ResultUtils.entryOf
import akka.persistence.r2dbc.snapshot.{SnapshotEntry, SnapshotStoreDao}
import akka.stream.scaladsl.Source
import io.netty.buffer.ByteBufUtil
import java.lang.{Long => JLong}

private[snapshot] object MySqlSnapshotStoreDao {

  def fetchSnapshotQuery(persistenceId: String, criteria: SnapshotSelectionCriteria): String =
    "SELECT persistence_id, sequence_nr, instant, snapshot FROM snapshot" +
        s" WHERE persistence_id = '$persistenceId'" +
        selectionCriteria(criteria) + " ORDER BY sequence_nr DESC LIMIT 1"

  def upsertSnapshotQuery(entry: SnapshotEntry): String = {
    val snapshotHex = ByteBufUtil.hexDump(entry.snapshot)
    "INSERT INTO snapshot (persistence_id, sequence_nr, instant, snapshot) VALUES (" +
        s"'${entry.persistenceId}', ${entry.sequenceNr}, ${entry.instant}, x'$snapshotHex')" +
        s" ON DUPLICATE KEY UPDATE instant = ${entry.instant}, snapshot = x'$snapshotHex'"
  }

  def deleteSnapshotQuery(persistenceId: String, seqNr: Long): String =
    s"DELETE FROM snapshot WHERE persistence_id = '$persistenceId' AND sequence_nr = $seqNr"

  def deleteSnapshotQuery(persistenceId: String, criteria: SnapshotSelectionCriteria): String =
    s"DELETE FROM snapshot WHERE persistence_id = '$persistenceId' " + selectionCriteria(criteria)

  private def selectionCriteria(criteria: SnapshotSelectionCriteria) = {
    var query = ""
    if (criteria.maxSequenceNr != JLong.MAX_VALUE) query += " AND sequence_nr <= " + criteria.maxSequenceNr
    if (criteria.maxTimestamp != JLong.MAX_VALUE) query += " AND instant <= " + criteria.maxTimestamp
    if (criteria.minSequenceNr != 0L) query += " AND sequence_nr => " + criteria.minSequenceNr
    if (criteria.minTimestamp != 0L) query += " AND instant => " + criteria.minTimestamp
    query
  }

}

private[snapshot] final class MySqlSnapshotStoreDao(val r2dbc: R2dbc) extends SnapshotStoreDao {

  import MySqlSnapshotStoreDao._

  override def fetchSnapshot(persistenceId: String, criteria: SnapshotSelectionCriteria): Source[SnapshotEntry, NotUsed] =
    Source.fromPublisher(r2dbc.withHandle(_.executeQuery(fetchSnapshotQuery(persistenceId, criteria), entryOf)))

  override def save(entry: SnapshotEntry): Source[Int, NotUsed] = Source.fromPublisher(
    r2dbc.withHandle(_.executeQuery(upsertSnapshotQuery(entry), _.getRowsUpdated))
  ).map(_.toInt)

  override def deleteSnapshot(persistenceId: String, seqNr: Long): Source[Int, NotUsed] = Source.fromPublisher(
    r2dbc.withHandle(_.executeQuery(deleteSnapshotQuery(persistenceId, seqNr), _.getRowsUpdated))
  ).map(_.toInt)

  override def deleteSnapshot(persistenceId: String, criteria: SnapshotSelectionCriteria): Source[Int, NotUsed] =
    Source.fromPublisher(r2dbc.withHandle(_.executeQuery(deleteSnapshotQuery(persistenceId, criteria), _.getRowsUpdated)))
        .map(_.toInt)

}
