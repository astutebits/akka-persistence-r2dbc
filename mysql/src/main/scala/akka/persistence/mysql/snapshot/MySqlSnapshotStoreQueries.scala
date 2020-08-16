package akka.persistence.mysql.snapshot

import akka.persistence.SnapshotSelectionCriteria
import akka.persistence.r2dbc.snapshot.SnapshotEntry
import io.netty.buffer.ByteBufUtil
import java.lang.{Long => JLong}

private[snapshot] object MySqlSnapshotStoreQueries {

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
