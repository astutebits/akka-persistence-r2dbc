package akka.persistence.r2dbc.snapshot

import java.lang.{Long => JLong}

private[akka] object SnapshotEntry {

  def of(pId: String, seqNr: JLong, timestamp: JLong, snapshot: Array[Byte]): SnapshotEntry =
    SnapshotEntry(pId, seqNr.toLong, timestamp.toLong, snapshot)

}

private[akka] final case class SnapshotEntry(
    persistenceId: String,
    sequenceNr: Long,
    instant: Long,
    snapshot: Array[Byte]
)
