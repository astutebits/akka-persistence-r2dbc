package akka.persistence.r2dbc.snapshot

import akka.persistence.serialization.Snapshot
import akka.persistence.{SelectedSnapshot, SnapshotMetadata}
import akka.serialization.Serialization

import scala.util.Try

private[snapshot] trait SnapshotSerializer {
  def serialize(metadata: SnapshotMetadata, snapshot: Any): Try[SnapshotEntry]

  def deserialize(entry: SnapshotEntry): Try[SelectedSnapshot]
}

private[snapshot] object SnapshotSerializerImpl {
  def apply(serialization: Serialization): SnapshotSerializer = new SnapshotSerializerImpl(serialization)
}

private[snapshot] final class SnapshotSerializerImpl private(serialization: Serialization)
    extends SnapshotSerializer {

  def serialize(metadata: SnapshotMetadata, snapshot: Any): Try[SnapshotEntry] = serialization
      .serialize(Snapshot(snapshot))
      .map(SnapshotEntry(metadata.persistenceId, metadata.sequenceNr, metadata.timestamp, _))

  def deserialize(entry: SnapshotEntry): Try[SelectedSnapshot] = serialization
      .deserialize(entry.snapshot, classOf[Snapshot])
      .map(snapshot => {
        val metadata = SnapshotMetadata(entry.persistenceId, entry.sequenceNr, entry.instant)
        SelectedSnapshot(metadata, snapshot.data)
      })
}
