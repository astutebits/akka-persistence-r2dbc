package akka.persistence.mysql.journal

import akka.persistence.r2dbc.journal.JournalEntry
import io.netty.buffer.ByteBufUtil
import java.lang.{Long => JLong, Integer => JInt}
import java.util.stream.Collectors
import java.util.{List => JList, Set => JSet}
import reactor.util.function.{Tuple2, Tuples}

private[akka] object MySqlJournalQueries {
  private val InsertBindingSize = 8

  def insertEventQueryBindings(entries: JList[JournalEntry]): Array[Object] = {
    val bindings: Array[Object] = new Array[Object](entries.size() * InsertBindingSize)

    for (i <- 0 until entries.size()) {
      bindings(i * InsertBindingSize + 0) = entries.get(i).persistenceId
      bindings(i * InsertBindingSize + 1) = entries.get(i).sequenceNr.asInstanceOf[JLong]
      bindings(i * InsertBindingSize + 2) = entries.get(i).timestamp.asInstanceOf[JLong]
      bindings(i * InsertBindingSize + 3) = entries.get(i).event
      bindings(i * InsertBindingSize + 4) = entries.get(i).eventManifest
      bindings(i * InsertBindingSize + 5) = entries.get(i).serId.asInstanceOf[JInt]
      bindings(i * InsertBindingSize + 6) = entries.get(i).serManifest
      bindings(i * InsertBindingSize + 7) = entries.get(i).writerUuid
    }

    bindings
  }

  def insertEventsBindingQuery(entries: JList[JournalEntry]): String = {
    val params = for (i <- 0 until entries.size() * InsertBindingSize) yield i
    s"INSERT INTO event (persistence_id, sequence_nr, timestamp, payload, manifest, ser_id, ser_manifest, writer_uuid) VALUES " +
        s"${params.sliding(InsertBindingSize, InsertBindingSize).map(i => s"(${i.map(_ => "?").mkString(",")})").mkString(",")}"
  }

  def insertEventsQuery(entries: JList[JournalEntry]): String =
    "INSERT INTO event (persistence_id, sequence_nr, timestamp, payload, manifest, ser_id, ser_manifest, writer_uuid) VALUES " + entries.stream
        .map(it => s"('${it.persistenceId}', ${it.sequenceNr}, ${it.timestamp}, x'${ByteBufUtil.hexDump(it.event)}', " +
            s"'${it.eventManifest}', ${it.serId}, '${it.serManifest}', '${it.writerUuid}')")
        .collect(Collectors.joining(",")) + ";"

  def insertTagsQuery(items: JList[Tuple2[JLong, JSet[String]]]): String =
    "INSERT INTO tag (event_id, tag) VALUES " + items.stream
        .flatMap(item => item.getT2.stream.map(tag => Tuples.of(item.getT1, tag)))
        .map((item: Tuple2[JLong, String]) => s"(${item.getT1},'${item.getT2}')")
        .collect(Collectors.joining(","))

  def findEventsQuery(persistenceId: String, fromSeqNr: Long, toSeqNr: Long, max: Long): String =
    "SELECT id, persistence_id, sequence_nr, timestamp, payload, manifest, ser_id, ser_manifest, writer_uuid FROM event" +
        s" WHERE deleted = false AND persistence_id = '$persistenceId'" +
        s" AND sequence_nr BETWEEN $fromSeqNr AND $toSeqNr ORDER BY sequence_nr ASC LIMIT $max"

  def markEventsAsDeletedQuery(persistenceId: String, toSeqNr: Long): String =
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
