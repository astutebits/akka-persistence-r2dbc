package akka.persistence.r2dbc.snapshot;

import static akka.persistence.r2dbc.snapshot.SnapshotStoreStatements.deleteSnapshotQuery;
import static akka.persistence.r2dbc.snapshot.SnapshotStoreStatements.deleteSnapshotsQuery;
import static akka.persistence.r2dbc.snapshot.SnapshotStoreStatements.fetchSnapshotQuery;
import static akka.persistence.r2dbc.snapshot.SnapshotStoreStatements.upsertSnapshotQuery;

import akka.persistence.SnapshotSelectionCriteria;
import io.r2dbc.postgresql.PostgresqlConnectionFactory;
import io.r2dbc.postgresql.api.PostgresqlResult;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public final class PostgresSnapshotStoreDao {

  private final PostgresqlConnectionFactory factory;

  public PostgresSnapshotStoreDao(PostgresqlConnectionFactory factory) {
    this.factory = factory;
  }

  public Flux<Integer> save(SnapshotEntry entry) {
    return factory.create()
        .flatMapMany(connection -> connection.createStatement(upsertSnapshotQuery(entry)).execute()
            .flatMap(PostgresqlResult::getRowsUpdated)
            .concatWith(Flux.from(connection.close()).then(Mono.empty()))
            .onErrorResume(throwable -> Flux.from(connection.close()).then(Mono.error(throwable)))
        );
  }

  public Flux<SnapshotEntry> fetchSnapshot(
      String persistenceId,
      SnapshotSelectionCriteria criteria
  ) {
    return factory.create().flatMapMany(connection -> connection
        .createStatement(fetchSnapshotQuery(persistenceId, criteria))
        .execute()
        .flatMap(result -> result.map((row, metadata) -> SnapshotEntry.of(
            row.get("persistence_id", String.class),
            row.get("sequence_number", Long.class),
            row.get("timestamp", Long.class),
            row.get("snapshot", byte[].class)
        )))
        .concatWith(Flux.from(connection.close()).then(Mono.empty()))
        .onErrorResume(throwable -> Flux.from(connection.close()).then(Mono.error(throwable)))
    );
  }

  public Flux<Integer> deleteSnapshot(String persistenceId, Long seqNr) {
    return factory.create().flatMapMany(connection -> connection
        .createStatement(deleteSnapshotQuery(persistenceId, seqNr))
        .execute()
        .flatMap(PostgresqlResult::getRowsUpdated)
        .concatWith(Flux.from(connection.close()).then(Mono.empty()))
        .onErrorResume(throwable -> Flux.from(connection.close()).then(Mono.error(throwable)))
    );
  }

  public Flux<Integer> deleteSnapshots(String persistenceId, SnapshotSelectionCriteria criteria) {
    return factory.create().flatMapMany(connection -> connection
        .createStatement(deleteSnapshotsQuery(persistenceId, criteria))
        .execute()
        .flatMap(PostgresqlResult::getRowsUpdated)
        .concatWith(Flux.from(connection.close()).then(Mono.empty()))
        .onErrorResume(throwable -> Flux.from(connection.close()).then(Mono.error(throwable)))
    );
  }

}
