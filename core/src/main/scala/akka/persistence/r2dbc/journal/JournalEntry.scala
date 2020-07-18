package akka.persistence.r2dbc.journal

import io.r2dbc.spi.Result
import java.lang.{Long => JLong}
import org.reactivestreams.Publisher

private[akka] object JournalEntry {

  def apply(
      persistenceId: String,
      sequenceNr: Long,
      event: Array[Byte],
      tags: Set[String]
  ): JournalEntry =
    new JournalEntry(Long.MinValue, persistenceId, sequenceNr, event, tags)

  def of(index: JLong, persistenceId: String, seqNr: JLong, event: Array[Byte]): JournalEntry =
    JournalEntry(index.toLong, persistenceId, seqNr.toLong, event)

}

/**
 * An event entry.
 *
 * @param id the unique id of the row
 * @param persistenceId Persistent ID that journals a persistent message.
 * @param sequenceNr This persistent message's sequence number.
 * @param event This persistent message's payload (the event).
 * @param tags A set of tags.
 * @param deleted Flag to indicate the logical tuple has been deleted.
 */
private[akka] final case class JournalEntry(
    id: Long,
    persistenceId: String,
    sequenceNr: Long,
    event: Array[Byte],
    tags: Set[String] = Set.empty,
    deleted: Boolean = false
)
