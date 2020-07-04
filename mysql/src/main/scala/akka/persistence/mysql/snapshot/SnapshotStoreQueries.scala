package akka.persistence.mysql.snapshot

import akka.persistence.SnapshotSelectionCriteria
import akka.persistence.r2dbc.snapshot.SnapshotEntry
import io.netty.buffer.ByteBufUtil
import java.lang.{Long => JLong}

private[akka] object SnapshotStoreQueries {

  private def selectionCriteria(criteria: SnapshotSelectionCriteria) = {
    var query = ""
    if (criteria.maxSequenceNr != JLong.MAX_VALUE) query += " AND seq_nr <= " + criteria.maxSequenceNr
    if (criteria.maxTimestamp != JLong.MAX_VALUE) query += " AND time <= " + criteria.maxTimestamp
    if (criteria.minSequenceNr != 0L) query += " AND seq_nj => " + criteria.minSequenceNr
    if (criteria.minTimestamp != 0L) query += " AND time => " + criteria.minTimestamp
    query
  }

  def fetchSnapshotQuery(persistenceId: String, criteria: SnapshotSelectionCriteria): String =
    "SELECT persistence_id, seq_nr, time, snapshot FROM snapshot" +
        s" WHERE persistence_id = '$persistenceId'" +
        selectionCriteria(criteria) + " ORDER BY seq_nr DESC LIMIT 1"

  def upsertSnapshotQuery(entry: SnapshotEntry): String = {
    val snapshotHex = ByteBufUtil.hexDump(entry.snapshot)
    "INSERT INTO snapshot (persistence_id, seq_nr, time, snapshot) VALUES (" +
        s"'${entry.persistenceId}', ${entry.sequenceNumber}, ${entry.timestamp}, x'$snapshotHex')" +
        s" ON DUPLICATE KEY UPDATE time = ${entry.timestamp}, snapshot = x'$snapshotHex'"
  }

  def deleteSnapshotQuery(persistenceId: String, seqNr: Long): String =
    s"DELETE FROM snapshot WHERE persistence_id = '$persistenceId' AND seq_nr = $seqNr"

  def deleteSnapshotQuery(persistenceId: String, criteria: SnapshotSelectionCriteria): String =
    s"DELETE FROM snapshot WHERE persistence_id = '$persistenceId' " + selectionCriteria(criteria)

}
