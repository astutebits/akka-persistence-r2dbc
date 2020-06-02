package akka.persistence.r2dbc.snapshot

import akka.persistence.serialization.Snapshot
import akka.persistence.{SelectedSnapshot, SnapshotMetadata}
import akka.serialization.Serialization

import scala.util.Try

trait SnapshotSerializer {
  def serialize(metadata: SnapshotMetadata, snapshot: Any): Try[SnapshotEntry]

  def deserialize(entry: SnapshotEntry): Try[SelectedSnapshot]
}

private[akka] object SnapshotSerializerImpl {
  def apply(serialization: Serialization): SnapshotSerializer = new SnapshotSerializerImpl(serialization)
}

private[akka] final class SnapshotSerializerImpl private(serialization: Serialization)
    extends SnapshotSerializer {

  def serialize(metadata: SnapshotMetadata, snapshot: Any): Try[SnapshotEntry] = serialization
      .serialize(Snapshot(snapshot))
      .map(SnapshotEntry(metadata.persistenceId, metadata.sequenceNr, metadata.timestamp, _))

  def deserialize(entry: SnapshotEntry): Try[SelectedSnapshot] = serialization
      .deserialize(entry.snapshot, classOf[Snapshot])
      .map(snapshot => {
        val metadata = SnapshotMetadata(entry.persistenceId, entry.sequenceNumber, entry.timestamp)
        SelectedSnapshot(metadata, snapshot.data)
      })
}
