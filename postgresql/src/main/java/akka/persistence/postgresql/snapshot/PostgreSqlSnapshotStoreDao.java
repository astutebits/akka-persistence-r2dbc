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
