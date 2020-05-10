package akka.persistence.r2dbc.journal;

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

  static String findEventsQuery(String persistenceId, long fromSeqNr, long toSeqNr) {
    return "SELECT index, persistence_id, sequence_nr, event FROM journal_event"
        + " WHERE deleted = false AND persistence_id = '" + persistenceId + "'"
        + " AND sequence_nr BETWEEN " + fromSeqNr + " AND " + toSeqNr
        + " ORDER BY sequence_nr ASC";
  }

}
