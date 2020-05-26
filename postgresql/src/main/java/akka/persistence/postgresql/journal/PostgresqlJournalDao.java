package akka.persistence.postgresql.journal;

import static akka.persistence.postgresql.journal.JournalStatements.deleteEventsQuery;
import static akka.persistence.postgresql.journal.JournalStatements.findEventsQuery;
import static akka.persistence.postgresql.journal.JournalStatements.highestMarkedSeqNrQuery;
import static akka.persistence.postgresql.journal.JournalStatements.highestSeqNrQuery;
import static akka.persistence.postgresql.journal.JournalStatements.insertEventsQuery;
import static akka.persistence.postgresql.journal.JournalStatements.insertTagsQuery;
import static akka.persistence.postgresql.journal.JournalStatements.markEventsAsDeletedQuery;

import akka.NotUsed;
import akka.persistence.r2dbc.client.R2dbc;
import akka.persistence.r2dbc.journal.AbstractJournalDao;
import akka.persistence.r2dbc.journal.JournalEntry;
import akka.stream.scaladsl.Source;
import io.r2dbc.spi.Result;
import java.util.List;
import java.util.stream.Collectors;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.util.function.Tuple2;
import scala.jdk.javaapi.CollectionConverters;

public final class PostgresqlJournalDao extends AbstractJournalDao {

  private final R2dbc r2dbc;

  public PostgresqlJournalDao(R2dbc r2dbc) {
    this.r2dbc = r2dbc;
  }

  private static Publisher<Long> toIndex(Result result) {
    return result.map((row, metadata) -> row.get("index", Long.class));
  }

  private static Publisher<Long> toSeqNr(Result result) {
    return result.map((row, metadata) -> row.get("sequence_nr", Long.class));
  }

  @Override
  public Source<Integer, NotUsed> doWriteEvents(List<JournalEntry> events) {
    Flux<Integer> flux = r2dbc.inTransaction(handle ->
        handle.executeQuery(insertEventsQuery(events), PostgresqlJournalDao::toIndex)
            .zipWithIterable(events.stream()
                .map(event -> CollectionConverters.asJava(event.tags()))
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
    Flux<JournalEntry> flux = r2dbc.withHandle(handle ->
        handle.executeQuery(findEventsQuery(persistenceId, fromSeqNr, toSeqNr, max),
            result -> result.map((row, metadata) -> JournalEntry.of(
                row.get("index", Long.class),
                row.get("persistence_id", String.class),
                row.get("sequence_nr", Long.class),
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
        handle.executeQuery(markEventsAsDeletedQuery(persistenceId, toSeqNr), Result::getRowsUpdated)
            .thenMany(handle.executeQuery(highestMarkedSeqNrQuery(persistenceId),
                PostgresqlJournalDao::toSeqNr)
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
        handle.executeQuery(
            highestSeqNrQuery(persistenceId, fromSeqNr), PostgresqlJournalDao::toSeqNr
        )
            .defaultIfEmpty(0L)
    );
    return Source.fromPublisher(flux);
  }

}
