package akka.persistence.mysql.snapshot;

import static akka.persistence.mysql.snapshot.SnapshotStoreQueries.deleteSnapshotQuery;
import static akka.persistence.mysql.snapshot.SnapshotStoreQueries.fetchSnapshotQuery;
import static akka.persistence.mysql.snapshot.SnapshotStoreQueries.upsertSnapshotQuery;

import akka.NotUsed;
import akka.persistence.SnapshotSelectionCriteria;
import akka.persistence.r2dbc.client.R2dbc;
import akka.persistence.r2dbc.snapshot.AbstractSnapshotStoreDao;
import akka.persistence.r2dbc.snapshot.SnapshotEntry;
import akka.stream.scaladsl.Source;
import io.r2dbc.spi.Result;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

final class MySqlSnapshotStoreDao extends AbstractSnapshotStoreDao {

  private final R2dbc r2dbc;

  private static Publisher<SnapshotEntry> entryOf(Result result) {
    return result.map((row, metadata) -> SnapshotEntry.of(
        row.get("persistence_id", String.class),
        row.get("seq_nr", Long.class),
        row.get("time", Long.class),
        row.get("snapshot", byte[].class)
    ));
  }

  public MySqlSnapshotStoreDao(R2dbc r2dbc) {
    this.r2dbc = r2dbc;
  }

  @Override
  public Source<SnapshotEntry, NotUsed> doFetchSnapshot(
      String persistenceId,
      SnapshotSelectionCriteria criteria
  ) {
    Flux<SnapshotEntry> flux = r2dbc.withHandle(handle -> handle.executeQuery(
        fetchSnapshotQuery(persistenceId, criteria),
        MySqlSnapshotStoreDao::entryOf
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
  public Source<Integer, NotUsed> doDeleteSnapshot(String persistenceId,
      SnapshotSelectionCriteria criteria) {
    Flux<Integer> flux = r2dbc.inTransaction(handle -> handle.executeQuery(
        deleteSnapshotQuery(persistenceId, criteria),
        Result::getRowsUpdated
    ));
    return Source.fromPublisher(flux);
  }

}
