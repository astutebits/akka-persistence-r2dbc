package akka.persistence.r2dbc;

import io.netty.buffer.ByteBufUtil;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

public final class JournalStatements {

  private JournalStatements() {
  }

  static String insertEventsQuery(List<JournalEntry> events) {
    return "INSERT INTO journal_event (ordering, persistence_id, sequence_number, message) VALUES "
        + events.stream()
        .map(event -> "("
            + "DEFAULT,"
            + "'" + event.persistenceId() + "',"
            + event.sequenceNumber() + ","
            + "'\\x" + ByteBufUtil.hexDump(event.message()) + "'"
            + ")")
        .collect(Collectors.joining(","))
        + " RETURNING ordering;";
  }

  static String insertTagsQuery(List<Tuple2<Long, Set<String>>> items) {
    return "INSERT INTO tag (id, event_id, tag) VALUES " + items.stream()
        .flatMap(item -> item.getT2().stream().map(tag -> Tuples.of(item.getT1(), tag)))
        .map(item -> "(DEFAULT," + item.getT1() + ",'" + item.getT2() + "')")
        .collect(Collectors.joining(","));
  }

  static String markEventsAsDeleted(String persistenceId, Long toSeqNr) {
    return "UPDATE journal_event SET deleted = true"
        + " WHERE persistence_id = '" + persistenceId + "' AND sequence_number <= " + toSeqNr;
  }

  static String highestMarkedSeqNrQuery(String persistenceId) {
    return "SELECT sequence_number FROM journal_event"
        + " WHERE persistence_id = '" + persistenceId + "' AND deleted = true"
        + " ORDER BY sequence_number DESC LIMIT 1";
  }

  static String deleteEventsQuery(String persistenceId, Long toSeqNr) {
    return "DELETE FROM journal_event WHERE persistence_id = '" + persistenceId + "'"
        + " AND sequence_number <= " + toSeqNr;
  }

  static String highestSeqNrQuery(String persistenceId, Long fromSeqNr) {
    return "SELECT sequence_number FROM journal_event"
        + " WHERE persistence_id = '" + persistenceId + "'"
        + " AND sequence_number >= " + fromSeqNr
        + " ORDER BY sequence_number DESC LIMIT 1";
  }

  static String findEventsQuery(String persistenceId, long fromSeqNr, long toSeqNr) {
    return "SELECT ordering, persistence_id, sequence_number, message FROM journal_event"
        + " WHERE deleted = false AND persistence_id = '" + persistenceId + "'"
        + " AND sequence_number BETWEEN " + fromSeqNr + " AND " + toSeqNr
        + " ORDER BY sequence_number ASC";
  }

}
