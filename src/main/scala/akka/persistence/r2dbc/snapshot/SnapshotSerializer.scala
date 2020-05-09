package akka.persistence.r2dbc.snapshot

import akka.persistence.{SelectedSnapshot, SnapshotMetadata}
import akka.persistence.serialization.Snapshot
import akka.serialization.Serialization
import scala.util.Try

class SnapshotSerializer(serialization: Serialization) {

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
