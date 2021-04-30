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

package akka.persistence.r2dbc.journal

import akka.persistence.PersistentRepr
import java.lang.{ Integer => JInt, Long => JLong }
import java.time.Instant

private[akka] object JournalEntry {

  /**
   * Extends a journal message with Tagging and Atomic Projections,
   * the outcome of [[PersistenceReprSerializer.serialize()]] call.
   *
   * @param repr Representation of a persistent message
   * @param serId The ID of the serializer.
   * @param serManifest The serializer type hint.
   * @param bytes The serialized message payload.
   * @param tags Optional tags for `ReadJournal` queries.
   * @param projection Optional projection to write atomically when persisting the message entry.
   */
  def apply(
      repr: PersistentRepr,
      serId: Int,
      serManifest: String,
      bytes: Array[Byte],
      tags: Set[String],
      projection: Option[String]): JournalEntry = JournalEntry(
    Long.MinValue,
    repr.persistenceId,
    repr.sequenceNr,
    bytes,
    repr.writerUuid,
    repr.manifest,
    repr.timestamp,
    serId,
    serManifest,
    tags,
    repr.deleted,
    projection)

  def of(
      index: JLong,
      persistenceId: String,
      seqNr: JLong,
      timestamp: JLong,
      event: Array[Byte],
      manifest: String,
      serId: JInt,
      serManifest: String,
      writerUuid: String): JournalEntry = {
    JournalEntry(
      id = index.toLong,
      persistenceId = persistenceId,
      sequenceNr = seqNr.toLong,
      timestamp = timestamp,
      event = event,
      eventManifest = manifest,
      serId = serId,
      serManifest = serManifest,
      writerUuid = writerUuid)
  }

}

/**
 * An event entry.
 *
 * @param id The unique id of the row.
 * @param persistenceId Persistent ID that journals a persistent message.
 * @param sequenceNr This persistent message's sequence number.
 * @param event This persistent message's payload (the event).
 * @param writerUuid Unique identifier of the writing persistent actor.
 * @param eventManifest The event adapter manifest for the event if available. May be `""` if event
 * adapter manifest is not used.
 * @param timestamp The `timestamp` is the time the event was stored, in milliseconds since midnight,
 * January 1, 1970 UTC.
 * @param serId A unique value used to identify the Serializer implementation.
 * @param serManifest A type hint for the Serializer.
 * @param tags A set of tags.
 * @param deleted Flag to indicate the logical tuple has been deleted.
 */
final private[akka] case class JournalEntry(
    id: Long,
    persistenceId: String,
    sequenceNr: Long,
    event: Array[Byte],
    writerUuid: String = "",
    eventManifest: String = "",
    timestamp: Long = Instant.now.toEpochMilli,
    serId: Int = 0,
    serManifest: String = "",
    tags: Set[String] = Set.empty,
    deleted: Boolean = false,
    projected: Option[String] = None)
