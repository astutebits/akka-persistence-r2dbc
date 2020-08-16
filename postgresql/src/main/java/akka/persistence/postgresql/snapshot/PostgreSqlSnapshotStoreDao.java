package akka.persistence.postgresql.snapshot;

import static akka.persistence.postgresql.snapshot.PostgreSqlSnapshotStoreQueries.deleteSnapshotQuery;
import static akka.persistence.postgresql.snapshot.PostgreSqlSnapshotStoreQueries.fetchSnapshotQuery;
import static akka.persistence.postgresql.snapshot.PostgreSqlSnapshotStoreQueries.upsertSnapshotQuery;

import akka.NotUsed;
import akka.persistence.SnapshotSelectionCriteria;
import akka.persistence.r2dbc.client.R2dbc;
import akka.persistence.r2dbc.snapshot.AbstractSnapshotStoreDao;
import akka.persistence.r2dbc.snapshot.ResultUtils;
import akka.persistence.r2dbc.snapshot.SnapshotEntry;
import akka.stream.scaladsl.Source;
import io.r2dbc.spi.Result;
import reactor.core.publisher.Flux;

final class PostgreSqlSnapshotStoreDao extends AbstractSnapshotStoreDao {

  private final R2dbc r2dbc;

  PostgreSqlSnapshotStoreDao(R2dbc r2dbc) {
    this.r2dbc = r2dbc;
  }

  @Override
  public Source<SnapshotEntry, NotUsed> doFetchSnapshot(
      String persistenceId,
      SnapshotSelectionCriteria criteria
  ) {
    Flux<SnapshotEntry> flux = r2dbc.withHandle(handle -> handle.executeQuery(
        fetchSnapshotQuery(persistenceId, criteria), ResultUtils::entryOf
    ));
    return Source.fromPublisher(flux);
  }

  @Override
  public Source<Integer, NotUsed> doSave(SnapshotEntry entry) {
    Flux<Integer> flux = r2dbc.inTransaction(handle -> handle.executeQuery(
        upsertSnapshotQuery(entry), Result::getRowsUpdated
    ));
    return Source.fromPublisher(flux);
  }

  @Override
  public Source<Integer, NotUsed> doDeleteSnapshot(String persistenceId, Long seqNr) {
    Flux<Integer> flux = r2dbc.inTransaction(handle -> handle.executeQuery(
        deleteSnapshotQuery(persistenceId, seqNr), Result::getRowsUpdated
    ));
    return Source.fromPublisher(flux);
  }

  @Override
  public Source<Integer, NotUsed> doDeleteSnapshot(
      String persistenceId,
      SnapshotSelectionCriteria criteria
  ) {
    Flux<Integer> flux = r2dbc.inTransaction(handle -> handle.executeQuery(
        deleteSnapshotQuery(persistenceId, criteria), Result::getRowsUpdated
    ));
    return Source.fromPublisher(flux);
  }

}
