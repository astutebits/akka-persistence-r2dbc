package akka.persistence.mysql.journal

import akka.persistence.r2dbc.journal.JournalEntry
import io.netty.buffer.ByteBufUtil
import java.lang.{Long => JLong}
import java.util.stream.Collectors
import java.util.{List => JList, Set => JSet}
import reactor.util.function.{Tuple2, Tuples}

private[akka] object JournalQueryStatements {

  def insertEventsQuery(events: JList[JournalEntry]): String = {
    "INSERT INTO journal_event (persistence_id, seq_nr, event) VALUES " + events.stream
        .map(entry => s"('${entry.persistenceId}',${entry.sequenceNr},x'${ByteBufUtil.hexDump(entry.event)}')")
        .collect(Collectors.joining(",")) + ";"
  }

  def insertTagsQuery(items: JList[Tuple2[JLong, JSet[String]]]): String = {
    "INSERT INTO event_tag (journal_event_id, tag) VALUES " + items.stream
        .flatMap(item => item.getT2.stream.map(tag => Tuples.of(item.getT1, tag)))
        .map((item: Tuple2[JLong, String]) => s"(${item.getT1},'${item.getT2}')")
        .collect(Collectors.joining(","))
  }

  def findEventsQuery(persistenceId: String, fromSeqNr: Long, toSeqNr: Long, max: Long): String =
    "SELECT id, persistence_id, seq_nr, event FROM journal_event" +
        s" WHERE deleted = false AND persistence_id = '$persistenceId'" +
        s" AND seq_nr BETWEEN $fromSeqNr AND $toSeqNr ORDER BY seq_nr ASC LIMIT $max"

  def markEventsAsDeletedQuery(persistenceId: String, toSeqNr: Long) =
    s"UPDATE journal_event SET deleted = true WHERE persistence_id = '$persistenceId' AND seq_nr <= $toSeqNr"

  def highestMarkedSeqNrQuery(persistenceId: String) =
    s"SELECT seq_nr FROM journal_event WHERE persistence_id = '$persistenceId' AND deleted = true ORDER BY seq_nr DESC LIMIT 1"

  def deleteEventsQuery(persistenceId: String, toSeqNr: Long) =
    s"DELETE FROM journal_event WHERE persistence_id = '$persistenceId' AND seq_nr <= $toSeqNr"

  def highestSeqNrQuery(persistenceId: String, fromSeqNr: Long): String =
    s"SELECT seq_nr FROM journal_event WHERE persistence_id = '$persistenceId'" +
        s" AND seq_nr >= $fromSeqNr ORDER BY seq_nr DESC LIMIT 1"

}
