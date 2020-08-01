package akka.persistence.r2dbc.journal

import java.lang.{Integer => JInt, Long => JLong}
import java.time.Instant

private[akka] object JournalEntry {

  def of(
      index: JLong,
      persistenceId: String,
      seqNr: JLong,
      timestamp: JLong,
      event: Array[Byte],
      manifest: String,
      serId: JInt,
      serManifest: String,
      writerUuid: String
  ): JournalEntry = {
    JournalEntry(
      id = index.toLong,
      persistenceId = persistenceId,
      sequenceNr = seqNr.toLong,
      timestamp = timestamp,
      event = event,
      eventManifest = manifest,
      serId = serId,
      serManifest = serManifest,
      writerUuid = writerUuid
    )
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
private[akka] final case class JournalEntry(
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
    deleted: Boolean = false
)
