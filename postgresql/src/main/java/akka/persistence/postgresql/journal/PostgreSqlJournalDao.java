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

package akka.persistence.postgresql.journal;

import static akka.persistence.postgresql.journal.PostgreSqlJournalQueries.deleteEventsQuery;
import static akka.persistence.postgresql.journal.PostgreSqlJournalQueries.findEventsQuery;
import static akka.persistence.postgresql.journal.PostgreSqlJournalQueries.highestMarkedSeqNrQuery;
import static akka.persistence.postgresql.journal.PostgreSqlJournalQueries.highestSeqNrQuery;
import static akka.persistence.postgresql.journal.PostgreSqlJournalQueries.insertEventsQuery;
import static akka.persistence.postgresql.journal.PostgreSqlJournalQueries.insertTagsQuery;
import static akka.persistence.postgresql.journal.PostgreSqlJournalQueries.markEventsAsDeletedQuery;
import static akka.persistence.r2dbc.journal.ResultUtils.toSeqId;

import akka.NotUsed;
import akka.persistence.r2dbc.client.Handle;
import akka.persistence.r2dbc.client.R2dbc;
import akka.persistence.r2dbc.journal.AbstractJournalDao;
import akka.persistence.r2dbc.journal.JournalDao;
import akka.persistence.r2dbc.journal.JournalEntry;
import akka.persistence.r2dbc.journal.ResultUtils;
import akka.stream.scaladsl.Source;
import io.r2dbc.spi.Result;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.util.function.Tuple2;
import scala.collection.JavaConverters;

/**
 * A {@link JournalDao} for PostgreSQL with <strong>r2dbc-postgresql</strong>
 *
 * @see <a href="https://github.com/r2dbc/r2dbc-postgresql">r2dbc-postgresql</a>
 */
final class PostgreSqlJournalDao extends AbstractJournalDao {

  private final R2dbc r2dbc;

  PostgreSqlJournalDao(R2dbc r2dbc) {
    this.r2dbc = r2dbc;
  }

  @Override
  public Source<Integer, NotUsed> doWriteEvents(List<JournalEntry> events) {
    Flux<Integer> flux = r2dbc.inTransaction(handle ->
        handle.executeQuery(insertEventsQuery(events), result -> ResultUtils.toSeqId(result, "id"))
            .zipWithIterable(events.stream()
                .map(event -> JavaConverters.setAsJavaSet(event.tags()))
                .collect(Collectors.toList())
            )
            .collectList()
            .filter(x -> x.stream().map(Tuple2::getT2).anyMatch(z -> !z.isEmpty()))
            .flatMapMany(eventTags ->
                handle.executeQuery(insertTagsQuery(eventTags), Result::getRowsUpdated)
            )
    ).defaultIfEmpty(0);
    return Source.fromPublisher(flux);
  }

  @Override
  public Source<JournalEntry, NotUsed> doFetchEvents(
      String persistenceId, Long fromSeqNr, Long toSeqNr, Long max
  ) {
    Function<Handle, Publisher<JournalEntry>> findEvents = handle -> handle.executeQuery(
        findEventsQuery(persistenceId, fromSeqNr, toSeqNr, max), ResultUtils::toJournalEntry
    );
    return Source.fromPublisher(r2dbc.withHandle(findEvents).take(max));
  }

  @Override
  public Source<Integer, NotUsed> doDeleteEvents(String persistenceId, Long toSeqNr) {
    Function<Handle, Flux<Integer>> markAsDelete = handle -> handle.executeQuery(
        markEventsAsDeletedQuery(persistenceId, toSeqNr), Result::getRowsUpdated
    );
    Function<Handle, Flux<Integer>> deleteMarked = handle -> handle.executeQuery(
        highestMarkedSeqNrQuery(persistenceId), result -> toSeqId(result, "sequence_nr")
    )
        .flatMap(seqNr -> handle.executeQuery(
            deleteEventsQuery(persistenceId, seqNr - 1), Result::getRowsUpdated
        ));
    return Source.fromPublisher(r2dbc.inTransaction(handle ->
        markAsDelete.apply(handle).thenMany(deleteMarked.apply(handle))
    ));
  }

  @Override
  public Source<Long, NotUsed> doReadHighestSequenceNr(String persistenceId, Long fromSeqNr) {
    Function<Handle, Publisher<Long>> readHighestSeqNr = handle -> handle.executeQuery(
        highestSeqNrQuery(persistenceId, fromSeqNr), result -> toSeqId(result, "sequence_nr")
    );
    return Source.fromPublisher(r2dbc.withHandle(readHighestSeqNr));
  }

}
