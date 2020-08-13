package akka.persistence.mysql.journal;

import static akka.persistence.mysql.journal.MySqlJournalQueries.deleteEventsQuery;
import static akka.persistence.mysql.journal.MySqlJournalQueries.findEventsQuery;
import static akka.persistence.mysql.journal.MySqlJournalQueries.highestMarkedSeqNrQuery;
import static akka.persistence.mysql.journal.MySqlJournalQueries.highestSeqNrQuery;
import static akka.persistence.mysql.journal.MySqlJournalQueries.insertEventQueryBindings;
import static akka.persistence.mysql.journal.MySqlJournalQueries.insertEventsQuery;
import static akka.persistence.mysql.journal.MySqlJournalQueries.insertEventsBindingQuery;
import static akka.persistence.mysql.journal.MySqlJournalQueries.insertTagsQuery;
import static akka.persistence.mysql.journal.MySqlJournalQueries.markEventsAsDeletedQuery;
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
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
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

  private static Publisher<Long> lastId(Result result) {
    return result.map((row, metadata) -> row.get(LAST_ID, Long.class));
  }

  private final R2dbc r2dbc;

  public MySqlJournalDao(R2dbc r2dbc) {
    this.r2dbc = r2dbc;
  }

  public Source<Integer, NotUsed> bindWriteEvents(List<JournalEntry> events) {
    Flux<Integer> flux = r2dbc.inTransaction(handle ->
            handle.executePreparedQuery(insertEventsBindingQuery(events), insertEventQueryBindings(events), Result::getRowsUpdated)
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

  public Source<Integer, NotUsed> noBindWriteEvents(List<JournalEntry> events) {
    Flux<Integer> flux = r2dbc.inTransaction(handle ->
            handle.executeQuery(insertEventsQuery(events), Result::getRowsUpdated)
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
  public Source<Integer, NotUsed> doWriteEvents(List<JournalEntry> events) {
    return noBindWriteEvents(events);
  }

  @Override
  public Source<JournalEntry, NotUsed> doFetchEvents(
      String persistenceId,
      Long fromSeqNr,
      Long toSeqNr,
      Long max
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
