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

package akka.persistence.mysql.query.scaladsl;

import static akka.persistence.mysql.query.scaladsl.MySqlReadJournalQueries.fetchByPersistenceIdQuery;
import static akka.persistence.mysql.query.scaladsl.MySqlReadJournalQueries.fetchByTagQuery;
import static akka.persistence.mysql.query.scaladsl.MySqlReadJournalQueries.fetchPersistenceIdsQuery;
import static akka.persistence.mysql.query.scaladsl.MySqlReadJournalQueries.findHighestIndexQuery;
import static akka.persistence.mysql.query.scaladsl.MySqlReadJournalQueries.findHighestSeqQuery;

import akka.NotUsed;
import akka.persistence.r2dbc.client.R2dbc;
import akka.persistence.r2dbc.journal.JournalEntry;
import akka.persistence.r2dbc.journal.ResultUtils;
import akka.persistence.r2dbc.query.AbstractQueryDao;
import akka.stream.scaladsl.Source;
import reactor.core.publisher.Flux;
import scala.Tuple2;

final class MySqlQueryDao extends AbstractQueryDao {

  private final R2dbc r2dbc;

  MySqlQueryDao(R2dbc r2dbc) {
    this.r2dbc = r2dbc;
  }

  @Override
  public Source<Tuple2<Long, String>, NotUsed> doFetchPersistenceIds(Long offset) {
    Flux<Tuple2<Long, String>> flux = r2dbc.withHandle(handle -> handle.executeQuery(
        fetchPersistenceIdsQuery(offset), ResultUtils::toPersistenceId
    ));
    return Source.fromPublisher(flux);
  }

  @Override
  public Source<JournalEntry, NotUsed> doFetchByPersistenceId(
      String persistenceId,
      Long fromSeqNr,
      Long toSeqNr
  ) {
    Flux<JournalEntry> flux = r2dbc.withHandle(handle -> handle.executeQuery(
        fetchByPersistenceIdQuery(persistenceId, fromSeqNr, toSeqNr), ResultUtils::toJournalEntry
    ));
    return Source.fromPublisher(flux);
  }

  @Override
  public Source<JournalEntry, NotUsed> doFetchByTag(String tag, Long fromSeqNr, Long toSeqNr) {
    Flux<JournalEntry> flux = r2dbc.withHandle(handle -> handle.executeQuery(
        fetchByTagQuery(tag, fromSeqNr, toSeqNr), ResultUtils::toJournalEntry
    ));
    return Source.fromPublisher(flux);
  }

  @Override
  public Source<Long, NotUsed> doFindHighestIndex(String tag) {
    Flux<Long> flux = r2dbc.withHandle(handle -> handle.executeQuery(
        findHighestIndexQuery(tag),
        result -> ResultUtils.toSeqId(result, "event_id")
    ));
    return Source.fromPublisher(flux);
  }

  @Override
  public Source<Long, NotUsed> doFindHighestSeq(String persistenceId) {
    Flux<Long> flux = r2dbc.withHandle(handle -> handle.executeQuery(
        findHighestSeqQuery(persistenceId),
        result -> ResultUtils.toSeqId(result, "sequence_nr")
    ));
    return Source.fromPublisher(flux);
  }

}
