package akka.persistence.r2dbc

import java.lang.{Long => JLong}

private[akka] object JournalEntry {

  def of(ordering: JLong, persistenceId: String, seqNr: JLong, message: Array[Byte]): JournalEntry =
    JournalEntry(ordering.toLong, deleted = false, persistenceId, seqNr.toLong, message)

}

private[akka] final case class JournalEntry(
    ordering: Long,
    deleted: Boolean,
    persistenceId: String,
    sequenceNumber: Long,
    message: Array[Byte],
    tags: Set[String] = Set.empty
)
