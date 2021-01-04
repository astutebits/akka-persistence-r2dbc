/*
 * Copyright 2020-2021 Borislav Borisov.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
