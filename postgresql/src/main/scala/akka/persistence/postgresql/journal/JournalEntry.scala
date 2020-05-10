package akka.persistence.postgresql.journal

import java.lang.{Long => JLong}

private[akka] object JournalEntry {

  def of(index: JLong, persistenceId: String, seqNr: JLong, event: Array[Byte]): JournalEntry =
    JournalEntry(index.toLong, deleted = false, persistenceId, seqNr.toLong, event)

}

private[akka] final case class JournalEntry(
    index: Long,
    deleted: Boolean,
    persistenceId: String,
    sequenceNr: Long,
    event: Array[Byte],
    tags: Set[String] = Set.empty
)
