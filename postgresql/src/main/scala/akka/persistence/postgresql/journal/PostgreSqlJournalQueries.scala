/*
 * Copyright 2020 Borislav Borisov.
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

import java.lang.{Long => JLong}
import java.util.{List => JList, Set => JSet}

import akka.persistence.r2dbc.journal.JournalEntry
import io.netty.buffer.ByteBufUtil.hexDump
import reactor.util.function.Tuple2

import scala.collection.JavaConverters._

private[journal] object PostgreSqlJournalQueries {

  def insertEntriesQuery(entries: JList[JournalEntry]): String = {
    val projections = entries.asScala.flatMap(it => it.projected).reduceOption(_ + ";" + _)
    val events = "INSERT INTO event (id, persistence_id, sequence_nr, timestamp, payload, manifest, ser_id, ser_manifest, writer_uuid) VALUES " + entries.asScala
        .map(it => s"(DEFAULT, '${it.persistenceId}', ${it.sequenceNr}, ${it.timestamp}, '\\x${hexDump(it.event)}', " +
            s"'${it.eventManifest}', ${it.serId}, '${it.serManifest}', '${it.writerUuid}')")
        .reduce(_ + "," + _) + " RETURNING id;"
    projections.fold(events)(_ + ";" + events)
  }

  def insertTagsQuery(items: JList[Tuple2[JLong, JSet[String]]]): String =
    "INSERT INTO tag (id, event_id, tag) VALUES " + items.asScala
        .flatMap(it => it.getT2.asScala.map(tag => (it.getT1, tag)))
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
