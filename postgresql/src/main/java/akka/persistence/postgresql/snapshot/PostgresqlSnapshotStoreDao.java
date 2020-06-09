package akka.persistence.postgresql.snapshot;


import akka.NotUsed;
import akka.persistence.SnapshotSelectionCriteria;
import akka.persistence.r2dbc.client.R2dbc;
import akka.persistence.r2dbc.snapshot.AbstractSnapshotStoreDao;
import akka.persistence.r2dbc.snapshot.SnapshotEntry;
import akka.stream.scaladsl.Source;
import io.netty.buffer.ByteBufUtil;
import io.r2dbc.spi.Result;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

public final class PostgresqlSnapshotStoreDao extends AbstractSnapshotStoreDao {

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

  private static String upsertSnapshotQuery(SnapshotEntry entry) {
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

  static String deleteSnapshotQuery(String persistenceId, SnapshotSelectionCriteria criteria) {
    return "DELETE FROM snapshot WHERE persistence_id = '" + persistenceId + "'"
        + selectionCriteria(criteria);
  }

  private static Publisher<SnapshotEntry> entryOf(Result result) {
    return result.map((row, metadata) -> SnapshotEntry.of(
        row.get("persistence_id", String.class),
        row.get("sequence_number", Long.class),
        row.get("timestamp", Long.class),
        row.get("snapshot", byte[].class)
    ));
  }

  private final R2dbc r2dbc;

  public PostgresqlSnapshotStoreDao(R2dbc r2dbc) {
    this.r2dbc = r2dbc;
  }

  @Override
  public Source<SnapshotEntry, NotUsed> doFetchSnapshot(
      String persistenceId,
      SnapshotSelectionCriteria criteria
  ) {
    Flux<SnapshotEntry> flux = r2dbc.withHandle(handle -> handle.executeQuery(
        fetchSnapshotQuery(persistenceId, criteria),
        PostgresqlSnapshotStoreDao::entryOf
    ));
    return Source.fromPublisher(flux);
  }

  @Override
  public Source<Integer, NotUsed> doSave(SnapshotEntry entry) {
    Flux<Integer> flux = r2dbc.inTransaction(handle ->
        handle.executeQuery(upsertSnapshotQuery(entry), Result::getRowsUpdated)
    );
    return Source.fromPublisher(flux);
  }

  @Override
  public Source<Integer, NotUsed> doDeleteSnapshot(String persistenceId, Long seqNr) {
    Flux<Integer> flux = r2dbc.inTransaction(handle -> handle.executeQuery(
        deleteSnapshotQuery(persistenceId, seqNr),
        Result::getRowsUpdated
    ));
    return Source.fromPublisher(flux);
  }

  @Override
  public Source<Integer, NotUsed> doDeleteSnapshot(
      String persistenceId,
      SnapshotSelectionCriteria criteria
  ) {
    Flux<Integer> flux = r2dbc.inTransaction(handle -> handle.executeQuery(
        deleteSnapshotQuery(persistenceId, criteria),
        Result::getRowsUpdated
    ));
    return Source.fromPublisher(flux);
  }

}
