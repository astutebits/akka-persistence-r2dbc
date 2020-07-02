package akka.persistence.mysql.journal;

import static akka.persistence.mysql.journal.JournalQueryStatements.deleteEventsQuery;
import static akka.persistence.mysql.journal.JournalQueryStatements.findEventsQuery;
import static akka.persistence.mysql.journal.JournalQueryStatements.highestMarkedSeqNrQuery;
import static akka.persistence.mysql.journal.JournalQueryStatements.highestSeqNrQuery;
import static akka.persistence.mysql.journal.JournalQueryStatements.insertEventsQuery;
import static akka.persistence.mysql.journal.JournalQueryStatements.insertTagsQuery;
import static akka.persistence.mysql.journal.JournalQueryStatements.markEventsAsDeletedQuery;

import akka.NotUsed;
import akka.persistence.r2dbc.client.R2dbc;
import akka.persistence.r2dbc.journal.AbstractJournalDao;
import akka.persistence.r2dbc.journal.JournalDao;
import akka.persistence.r2dbc.journal.JournalEntry;
import akka.stream.scaladsl.Source;
import io.r2dbc.spi.Result;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
import scala.jdk.javaapi.CollectionConverters;

/**
 * A {@link JournalDao} for MySQL with <strong>r2dbc-mysql</strong>
 *
 * @see <a href="https://github.com/mirromutth/r2dbc-mysql">r2dbc-mysql</a>
 */
final class MySqlJournalDao extends AbstractJournalDao {
  private static final String LAST_ID = "LAST_INSERT_ID()";

  private static Publisher<Long> toIndex(Result result) {
    return result.map((row, metadata) -> row.get("index", Long.class));
  }

  private static Publisher<Long> toSeqNr(Result result) {
    return result.map((row, metadata) -> row.get("seq_nr", Long.class));
  }

  private static Publisher<Long> lastId(Result result) {
    return result.map((row, metadata) -> row.get(LAST_ID, Long.class));
  }

  private final R2dbc r2dbc;

  public MySqlJournalDao(R2dbc r2dbc) {
    this.r2dbc = r2dbc;
  }

  @Override
  public Source<Integer, NotUsed> doWriteEvents(List<JournalEntry> events) {
    Flux<Integer> flux = r2dbc.inTransaction(handle ->
        handle.executeQuery(insertEventsQuery(events), MySqlJournalDao::toIndex)
            .thenMany(handle.executeQuery("SELECT " + LAST_ID, MySqlJournalDao::lastId))
            .flatMap(idx -> {
              AtomicLong index = new AtomicLong(idx);
              return Flux.fromStream(events.stream().map(entry ->
                  Tuples.of(index.getAndIncrement(), CollectionConverters.asJava(entry.tags())))
              );
            })
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
      String persistenceId,
      Long fromSeqNr,
      Long toSeqNr,
      Long max
  ) {
    Flux<JournalEntry> flux = r2dbc.withHandle(handle ->
        handle.executeQuery(findEventsQuery(persistenceId, fromSeqNr, toSeqNr, max),
            result -> result.map((row, metadata) -> JournalEntry.of(
                row.get("id", Long.class),
                row.get("persistence_id", String.class),
                row.get("seq_nr", Long.class),
                row.get("event", byte[].class)
            ))
        )
            .take(max)
    );
    return Source.fromPublisher(flux);
  }

  @Override
  public Source<Integer, NotUsed> doDeleteEvents(String persistenceId, Long toSeqNr) {
    Flux<Integer> flux = r2dbc.inTransaction(handle ->
        handle .executeQuery(markEventsAsDeletedQuery(persistenceId, toSeqNr), Result::getRowsUpdated)
            .thenMany(handle.executeQuery(highestMarkedSeqNrQuery(persistenceId),
                MySqlJournalDao::toSeqNr)
                .defaultIfEmpty(0L)
                .flatMap(seqNr ->
                    handle.executeQuery(deleteEventsQuery(persistenceId, seqNr - 1),
                        Result::getRowsUpdated)
                )
            )
    );
    return Source.fromPublisher(flux);
  }

  @Override
  public Source<Long, NotUsed> doReadHighestSequenceNr(String persistenceId, Long fromSeqNr) {
    Flux<Long> flux = r2dbc.withHandle(handle ->
        handle.executeQuery(highestSeqNrQuery(persistenceId, fromSeqNr), MySqlJournalDao::toSeqNr)
            .defaultIfEmpty(0L)
    );
    return Source.fromPublisher(flux);
  }

}
