/*
 * Copyright 2020-2021 Borislav Borisov.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package akka.persistence.postgresql.journal

import akka.NotUsed
import akka.persistence.r2dbc.client.{Handle, R2dbc}
import akka.persistence.r2dbc.journal.ResultUtils.{toJournalEntry, toSeqId}
import akka.persistence.r2dbc.journal.{JournalDao, JournalEntry}
import akka.stream.scaladsl.Source
import io.netty.buffer.ByteBufUtil.hexDump
import io.r2dbc.spi.Result
import java.util.{List => JList}
import reactor.core.publisher.Flux
import reactor.util.function.{Tuple2 => RTuple2}
import scala.collection.JavaConverters._

private[journal] object LenientPostgreSqlJournalDao {

  type EntryTags = JList[RTuple2[Long, Set[String]]]

  def insertEntriesQuery(entries: Seq[JournalEntry]): String = {
    val projections = entries.flatMap(it => it.projected.map(_._1)).reduceOption(_ + ";" + _)
    val events = "INSERT INTO event (id, persistence_id, sequence_nr, timestamp, payload, manifest, ser_id, ser_manifest, writer_uuid) VALUES " + entries
        .map(it => s"(DEFAULT, '${it.persistenceId}', ${it.sequenceNr}, ${it.timestamp}, '\\x${hexDump(it.event)}', " +
            s"'${it.eventManifest}', ${it.serId}, '${it.serManifest}', '${it.writerUuid}')")
        .reduce(_ + "," + _) + " RETURNING id;"
    projections.fold(events)(_ + ";" + events)
  }

  def insertTagsQuery(items: JList[RTuple2[Long, Set[String]]]): String =
    "INSERT INTO tag (id, event_id, tag) VALUES " + items.asScala
        .flatMap(it => it.getT2.map(tag => (it.getT1, tag)))
        .map(it => s"(DEFAULT,${it._1},'${it._2}')")
        .reduce(_ + "," + _)

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

/**
 * A [[JournalDao]] for PostgreSQL with `r2dbc-postgresql`.
 *
 * @see [[https://github.com/r2dbc/r2dbc-postgresql r2dbc-postgresql]] for more
 */
final class LenientPostgreSqlJournalDao(val r2dbc: R2dbc) extends JournalDao {

  import LenientPostgreSqlJournalDao._

  override def writeEvents(events: Seq[JournalEntry]): Source[Int, NotUsed] = {
    val flux: Flux[Integer] = r2dbc.inTransaction((handle: Handle) =>
      handle.executeQuery(insertEntriesQuery(events), toSeqId(_, "id"))
          .zipWithIterable(events.flatMap(it => Set(it.tags)).asJava)
          .collectList
          .filter((it: EntryTags) => it.asScala.map(_.getT2).exists(_.nonEmpty))
          .flatMapMany((eventTags: EntryTags) => handle.executeQuery(insertTagsQuery(eventTags), _.getRowsUpdated))
    )
    Source.fromPublisher(flux.defaultIfEmpty(0)).map(_.toInt)
  }

  override def fetchEvents(
      persistenceId: String,
      fromSeqNr: Long,
      toSeqNr: Long,
      max: Long
  ): Source[JournalEntry, NotUsed] = {
    val flux = r2dbc.withHandle(handle =>
      handle.executeQuery(findEventsQuery(persistenceId, fromSeqNr, toSeqNr, max), toJournalEntry(_))
    )
    Source.fromPublisher(flux.take(max))
  }

  override def deleteEvents(persistenceId: String, toSeqNr: Long): Source[Int, NotUsed] = {
    val markAsDelete = (handle: Handle) =>
      handle.executeQuery(markEventsAsDeletedQuery(persistenceId, toSeqNr), _.getRowsUpdated)
    val deleteMarked = (handle: Handle) =>
      handle.executeQuery(highestMarkedSeqNrQuery(persistenceId), toSeqId(_, "sequence_nr"))
          .flatMap((seqNr: Long) => handle.executeQuery(deleteEventsQuery(persistenceId, seqNr - 1), _.getRowsUpdated))
    Source.fromPublisher(r2dbc.inTransaction((handle: Handle) =>
      markAsDelete(handle).thenMany(deleteMarked(handle)))).map(_.toInt)
  }

  override def readHighestSequenceNr(persistenceId: String, fromSeqNr: Long): Source[Long, NotUsed] =
    Source.fromPublisher(r2dbc.withHandle(handle =>
      handle.executeQuery(highestSeqNrQuery(persistenceId, fromSeqNr), (result: Result) => toSeqId(result, "sequence_nr"))
    ))

}
