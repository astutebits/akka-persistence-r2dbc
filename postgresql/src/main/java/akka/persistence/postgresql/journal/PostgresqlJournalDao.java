package akka.persistence.postgresql.journal;

import akka.NotUsed;
import akka.persistence.r2dbc.client.R2dbc;
import akka.persistence.r2dbc.journal.AbstractJournalDao;
import akka.persistence.r2dbc.journal.JournalEntry;
import akka.stream.scaladsl.Source;
import io.netty.buffer.ByteBufUtil;
import io.r2dbc.spi.Result;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
import scala.jdk.javaapi.CollectionConverters;

public final class PostgresqlJournalDao extends AbstractJournalDao {

  static String insertEventsQuery(List<JournalEntry> events) {
    return "INSERT INTO journal_event (index, persistence_id, sequence_nr, event) VALUES "
        + events.stream()
        .map(event -> "("
            + "DEFAULT,"
            + "'" + event.persistenceId() + "',"
            + event.sequenceNr() + ","
            + "'\\x" + ByteBufUtil.hexDump(event.event()) + "'"
            + ")")
        .collect(Collectors.joining(","))
        + " RETURNING index;";
  }

  static String insertTagsQuery(List<Tuple2<Long, Set<String>>> items) {
    return "INSERT INTO tag (index, event_index, tag) VALUES " + items.stream()
        .flatMap(item -> item.getT2().stream().map(tag -> Tuples.of(item.getT1(), tag)))
        .map(item -> "(DEFAULT," + item.getT1() + ",'" + item.getT2() + "')")
        .collect(Collectors.joining(","));
  }

  static String markEventsAsDeleted(String persistenceId, Long toSeqNr) {
    return "UPDATE journal_event SET deleted = true"
        + " WHERE persistence_id = '" + persistenceId + "' AND sequence_nr <= " + toSeqNr;
  }

  static String highestMarkedSeqNrQuery(String persistenceId) {
    return "SELECT sequence_nr FROM journal_event"
        + " WHERE persistence_id = '" + persistenceId + "' AND deleted = true"
        + " ORDER BY sequence_nr DESC LIMIT 1";
  }

  static String deleteEventsQuery(String persistenceId, Long toSeqNr) {
    return "DELETE FROM journal_event WHERE persistence_id = '" + persistenceId + "'"
        + " AND sequence_nr <= " + toSeqNr;
  }

  static String highestSeqNrQuery(String persistenceId, Long fromSeqNr) {
    return "SELECT sequence_nr FROM journal_event"
        + " WHERE persistence_id = '" + persistenceId + "'"
        + " AND sequence_nr >= " + fromSeqNr
        + " ORDER BY sequence_nr DESC LIMIT 1";
  }

  static String findEventsQuery(String persistenceId, long fromSeqNr, long toSeqNr, long max) {
    return "SELECT index, persistence_id, sequence_nr, event FROM journal_event"
        + " WHERE deleted = false AND persistence_id = '" + persistenceId + "'"
        + " AND sequence_nr BETWEEN " + fromSeqNr + " AND " + toSeqNr
        + " ORDER BY sequence_nr ASC LIMIT " + max;
  }


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
        handle.executeQuery(markEventsAsDeleted(persistenceId, toSeqNr), Result::getRowsUpdated)
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
