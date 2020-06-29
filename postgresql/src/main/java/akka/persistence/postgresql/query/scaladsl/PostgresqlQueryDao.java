package akka.persistence.postgresql.query.scaladsl;

import static akka.persistence.postgresql.query.QueryStatements.fetchByPersistenceIdQuery;
import static akka.persistence.postgresql.query.QueryStatements.fetchByTagQuery;
import static akka.persistence.postgresql.query.QueryStatements.fetchPersistenceIdsQuery;
import static akka.persistence.postgresql.query.QueryStatements.findHighestIndexQuery;
import static akka.persistence.postgresql.query.QueryStatements.findHighestSeqQuery;

import akka.NotUsed;
import akka.persistence.r2dbc.client.R2dbc;
import akka.persistence.r2dbc.journal.JournalEntry;
import akka.persistence.r2dbc.query.AbstractQueryDao;
import akka.stream.scaladsl.Source;
import io.r2dbc.spi.Result;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import scala.Tuple2;

final class PostgresqlQueryDao extends AbstractQueryDao {

  private final R2dbc r2dbc;

  public PostgresqlQueryDao(R2dbc r2dbc) {
    this.r2dbc = r2dbc;
  }

  private static Publisher<JournalEntry> toJournalEntry(Result result) {
    return result.map((row, metadata) -> JournalEntry.of(
        row.get("index", Long.class),
        row.get("persistence_id", String.class),
        row.get("sequence_nr", Long.class),
        row.get("event", byte[].class)
    ));
  }

  private static Publisher<Tuple2<Long, String>> toPersistenceId(Result result) {
    return result.map((row, metadata) -> new Tuple2<>(
        row.get("index", Long.class),
        row.get("persistence_id", String.class)
    ));
  }

  @Override
  public Source<Tuple2<Long, String>, NotUsed> doFetchPersistenceIds(Long offset) {
    new Tuple2<>(1L, "five");
    Flux<Tuple2<Long, String>> flux = r2dbc.withHandle(handle -> handle.executeQuery(
        fetchPersistenceIdsQuery(offset), PostgresqlQueryDao::toPersistenceId
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
        fetchByPersistenceIdQuery(persistenceId, fromSeqNr, toSeqNr),
        PostgresqlQueryDao::toJournalEntry
    ));
    return Source.fromPublisher(flux);
  }

  @Override
  public Source<JournalEntry, NotUsed> doFetchByTag(String tag, Long fromIndex, Long toIndex) {
    Flux<JournalEntry> flux = r2dbc.withHandle(handle -> handle.executeQuery(
        fetchByTagQuery(tag, fromIndex, toIndex), PostgresqlQueryDao::toJournalEntry
    ));
    return Source.fromPublisher(flux);
  }

  @Override
  public Source<Long, NotUsed> doFindHighestIndex(String tag) {
    Flux<Long> flux = r2dbc.withHandle(handle -> handle.executeQuery(
        findHighestIndexQuery(tag),
        result -> result.map((row, metadata) -> {
          Long index = row.get("event_index", Long.class);
          return index == null ? 0 : index;
        })
    ));
    return Source.fromPublisher(flux);
  }

  @Override
  public Source<Long, NotUsed> doFindHighestSeq(String persistenceId) {
    Flux<Long> flux = r2dbc.withHandle(handle -> handle.executeQuery(
        findHighestSeqQuery(persistenceId),
        result -> result.map((row, metadata) -> {
          Long seq = row.get("sequence_nr", Long.class);
          return seq == null ? 0 : seq;
        })
    ));
    return Source.fromPublisher(flux);
  }

}
