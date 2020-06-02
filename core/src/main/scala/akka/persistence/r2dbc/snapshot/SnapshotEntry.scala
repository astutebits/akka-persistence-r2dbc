package akka.persistence.r2dbc.snapshot

import java.lang.{Long => JLong}
object SnapshotEntry {

  def of(pId: String, seqNr: JLong, timestamp: JLong, snapshot: Array[Byte]): SnapshotEntry =
    SnapshotEntry(pId, seqNr.toLong, timestamp.toLong, snapshot)

}

private[akka] final case class SnapshotEntry(
    persistenceId: String,
    sequenceNumber: Long,
    timestamp: Long,
    snapshot: Array[Byte]
)
