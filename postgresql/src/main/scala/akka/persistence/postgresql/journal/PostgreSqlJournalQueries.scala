package akka.persistence.postgresql.journal

import akka.persistence.r2dbc.journal.JournalEntry
import io.netty.buffer.ByteBufUtil.hexDump
import java.lang.{Long => JLong}
import java.util.stream.Collectors
import java.util.{List => JList, Set => JSet}
import reactor.util.function.{Tuple2, Tuples}

private[akka] object PostgreSqlJournalQueries {

  def insertEventsQuery(entries: JList[JournalEntry]): String =
    "INSERT INTO event (id, persistence_id, sequence_nr, timestamp, payload, manifest, ser_id, ser_manifest, writer_uuid) VALUES " + entries.stream
        .map(it => s"(DEFAULT, '${it.persistenceId}', ${it.sequenceNr}, ${it.timestamp}, '\\x${hexDump(it.event)}', " +
            s"'${it.eventManifest}', ${it.serId}, '${it.serManifest}', '${it.writerUuid}')")
        .collect(Collectors.joining(",")) + " RETURNING id;"

  def insertTagsQuery(items: JList[Tuple2[JLong, JSet[String]]]): String =
    "INSERT INTO tag (id, event_id, tag) VALUES " + items.stream
        .flatMap(item => item.getT2.stream.map((tag: String) => Tuples.of(item.getT1, tag)))
        .map((item: Tuple2[JLong, String]) => s"(DEFAULT,${item.getT1},'${item.getT2}')")
        .collect(Collectors.joining(","))

  def findEventsQuery(persistenceId: String, fromSeqNr: Long, toSeqNr: Long, max: Long): String =
    "SELECT id, persistence_id, sequence_nr, timestamp, payload, manifest, ser_id, ser_manifest, writer_uuid FROM event" +
        s" WHERE deleted = false AND persistence_id = '$persistenceId'" +
        s" AND sequence_nr BETWEEN $fromSeqNr AND $toSeqNr ORDER BY sequence_nr ASC LIMIT $max"

  def markEventsAsDeletedQuery(persistenceId: String, toSeqNr: Long) =
    s"UPDATE event SET deleted = true WHERE persistence_id = '$persistenceId' AND sequence_nr <= $toSeqNr"

  def highestMarkedSeqNrQuery(persistenceId: String): String =
    s"SELECT sequence_nr FROM event WHERE persistence_id = '$persistenceId'" +
        " AND deleted = true ORDER BY sequence_nr DESC LIMIT 1"

  def deleteEventsQuery(persistenceId: String, toSeqNr: Long): String =
    s"DELETE FROM event WHERE persistence_id = '$persistenceId' AND sequence_nr <= $toSeqNr"

  def highestSeqNrQuery(persistenceId: String, fromSeqNr: Long): String =
    s"SELECT sequence_nr FROM event WHERE persistence_id = '$persistenceId'" +
        s" AND sequence_nr >= $fromSeqNr ORDER BY sequence_nr DESC LIMIT 1"

}
