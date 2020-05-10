package akka.persistence.postgresql.journal;

import static akka.persistence.postgresql.journal.JournalStatements.deleteEventsQuery;
import static akka.persistence.postgresql.journal.JournalStatements.findEventsQuery;
import static akka.persistence.postgresql.journal.JournalStatements.highestMarkedSeqNrQuery;
import static akka.persistence.postgresql.journal.JournalStatements.highestSeqNrQuery;
import static akka.persistence.postgresql.journal.JournalStatements.insertEventsQuery;
import static akka.persistence.postgresql.journal.JournalStatements.insertTagsQuery;
import static akka.persistence.postgresql.journal.JournalStatements.markEventsAsDeleted;

import akka.Done;
import io.r2dbc.postgresql.PostgresqlConnectionFactory;
import io.r2dbc.postgresql.api.PostgresqlResult;
import java.util.List;
import java.util.stream.Collectors;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import scala.concurrent.Promise;
import scala.jdk.javaapi.CollectionConverters;

public final class PostgresJournalDao {

  private final PostgresqlConnectionFactory factory;

  public PostgresJournalDao(PostgresqlConnectionFactory factory) {
    this.factory = factory;
  }

  /**
   * Persists the journal events and any associated tags, and then uses the given promise to signal
   * back success of failure
   *
   * @param events the journal events
   * @param promise signal back promise
   */
  public Flux<Integer> writeEvents(List<JournalEntry> events, Promise<Done> promise) {
    return factory.create()
        .flatMapMany(connection -> connection.beginTransaction()
            .thenMany(
                connection.createStatement(insertEventsQuery(events))
                    .execute()
                    .flatMap(result -> result.map((row, metadata) ->
                        row.get("index", Long.class))
                    )
                    .zipWithIterable(events.stream()
                        .map(event -> CollectionConverters.asJava(event.tags()))
                        .collect(Collectors.toList())
                    )
                    .collectList()
                    .filter(x -> x.stream().map(Tuple2::getT2).anyMatch(z -> !z.isEmpty()))
                    .flatMapMany(eventTags -> connection.createStatement(insertTagsQuery(eventTags))
                        .execute()
                        .flatMap(PostgresqlResult::getRowsUpdated)
                    )
            )
            .concatWith(Flux.from(connection.commitTransaction())
                .doOnTerminate(() -> promise.success(Done.done()))
                .thenMany(Flux.from(connection.close()))
                .then(Mono.empty()))
            .onErrorResume(throwable -> Flux.from(connection.rollbackTransaction())
                .doOnTerminate(() -> promise.failure(throwable))
                .thenMany(Flux.from(connection.close()))
                .then(Mono.error(throwable))
            )
        );
  }

  public Flux<JournalEntry> fetchEvents(String persistenceId, Long fromSeqNr, Long toSeqNr, Long max) {
    return factory.create().flatMapMany(connection ->
        connection.createStatement(findEventsQuery(persistenceId, fromSeqNr, toSeqNr)).execute()
            .flatMap(result -> result.map((row, metadata) -> JournalEntry.of(
                row.get("index", Long.class),
                row.get("persistence_id", String.class),
                row.get("sequence_nr", Long.class),
                row.get("event", byte[].class)
            )))
            .take(max)
            .concatWith(Flux.from(connection.close()).then(Mono.empty()))
            .onErrorResume(throwable -> Flux.from(connection.close()).then(Mono.error(throwable)))
    );
  }

  /**
   * Deletes the journal events for the given persistent ID up to the provided sequence number
   * (inclusive).
   *
   * As required by the API, the message deletion doesn't affect the highest sequence number of
   * messages, and we maintain the highest sequence number by "marking" the journal event entry as
   * deleted. Instead of deleting it permanently.
   *
   * @param persistenceId the persistence id
   * @param toSequenceNr sequence number (inclusive)
   */
  public Flux<Integer> deleteEvents(String persistenceId, Long toSequenceNr) {
    return factory.create().flatMapMany(connection -> connection.beginTransaction()
        .thenMany(
            connection.createStatement(markEventsAsDeleted(persistenceId, toSequenceNr)).execute()
                .flatMap(__ ->
                    connection.createStatement(highestMarkedSeqNrQuery(persistenceId))
                        .execute()
                        .flatMap(result -> result.map((row, metadata) ->
                            row.get("sequence_nr", Long.class))
                        )
                )
                .defaultIfEmpty(0L)
                .flatMap(seqNr ->
                    connection.createStatement(deleteEventsQuery(persistenceId, seqNr - 1))
                        .execute()
                )
                .flatMap(PostgresqlResult::getRowsUpdated)
        )
        .concatWith(Flux.from(connection.commitTransaction())
            .thenMany(Flux.from(connection.close()))
            .then(Mono.empty()))
        .onErrorResume(throwable -> Flux.from(connection.rollbackTransaction())
            .thenMany(Flux.from(connection.close()))
            .then(Mono.error(throwable)))
    );
  }

  /**
   * Selects the event with the highest {@code sequence_nr} whose {@code deleted} set to true.
   *
   * @param persistenceId the persistence ID
   * @param fromSequenceNr sequence number (inclusive)
   */
  public Flux<Long> readHighestSequenceNr(String persistenceId, Long fromSequenceNr) {
    return factory.create().flatMapMany(connection ->
        connection.createStatement(highestSeqNrQuery(persistenceId, fromSequenceNr)).execute()
            .flatMap(result -> result.map(((row, metadata) ->
                row.get("sequence_nr", Long.class)))
            )
            .defaultIfEmpty(0L)
            .concatWith(Flux.from(connection.close()).then(Mono.empty()))
            .onErrorResume(throwable -> Flux.from(connection.close()).then(Mono.error(throwable)))
    );
  }

}
