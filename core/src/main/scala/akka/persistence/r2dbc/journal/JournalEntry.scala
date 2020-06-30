package akka.persistence.r2dbc.journal

import java.lang.{Long => JLong}

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

private[akka] final case class JournalEntry(
    index: Long,
    persistenceId: String,
    sequenceNr: Long,
    event: Array[Byte],
    tags: Set[String] = Set.empty,
    deleted: Boolean = false
)
