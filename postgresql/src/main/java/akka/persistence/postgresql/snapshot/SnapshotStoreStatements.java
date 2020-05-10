package akka.persistence.postgresql.snapshot;

import akka.persistence.SnapshotSelectionCriteria;
import io.netty.buffer.ByteBufUtil;

final class SnapshotStoreStatements {

  private SnapshotStoreStatements() {
  }

  static String upsertSnapshotQuery(SnapshotEntry entry) {
    final String snapshotHex = "\\x" + ByteBufUtil.hexDump(entry.snapshot());
    return "INSERT INTO snapshot (persistence_id, sequence_number, timestamp, snapshot) VALUES ("
        + "'" + entry.persistenceId() + "',"
        + entry.sequenceNumber() + ","
        + entry.timestamp() + ","
        + "'" + snapshotHex + "'"
        + ") ON CONFLICT (persistence_id, sequence_number) DO UPDATE SET "
        + "timestamp = " + entry.timestamp() + ","
        + "snapshot = '" + snapshotHex + "'";
  }

  // Note, the two criterion - minSeqNr and minTs - do not appear to be used anywhere. But we've
  // added them in case they are used in the future.
  static String fetchSnapshotQuery(String persistenceId, SnapshotSelectionCriteria criteria) {
    return "SELECT persistence_id, sequence_number, timestamp, snapshot FROM snapshot "
        + "WHERE persistence_id = '" + persistenceId + "'"
        + selectionCriteria(criteria)
        + " ORDER BY sequence_number DESC LIMIT 1";
  }

  static String deleteSnapshotQuery(String persistenceId, Long seqNr) {
    return "DELETE FROM snapshot WHERE persistence_id = '" + persistenceId + "' "
        + "AND sequence_number = " + seqNr;
  }

  static String deleteSnapshotsQuery(String persistenceId, SnapshotSelectionCriteria criteria) {
    return "DELETE FROM snapshot WHERE persistence_id = '" + persistenceId + "'"
        + selectionCriteria(criteria);
  }

  private static String selectionCriteria(SnapshotSelectionCriteria criteria) {
    String query = "";

    if (criteria.maxSequenceNr() != Long.MAX_VALUE) {
      query += " AND sequence_number <= " + criteria.maxSequenceNr();
    }

    if (criteria.maxTimestamp() != Long.MAX_VALUE) {
      query += " AND timestamp <= " + criteria.maxTimestamp();
    }

    if (criteria.minSequenceNr() != 0L) {
      query += " AND sequence_number => " + criteria.minSequenceNr();
    }

    if (criteria.minTimestamp() != 0L) {
      query += " AND timestamp => " + criteria.minTimestamp();
    }

    return query;
  }

}
