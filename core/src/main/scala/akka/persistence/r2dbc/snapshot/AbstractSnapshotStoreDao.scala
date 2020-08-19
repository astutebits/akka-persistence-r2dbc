/*
 * Copyright 2020 Borislav Borisov.
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

import java.lang.{Long => JLong}

import akka.NotUsed
import akka.persistence.SnapshotSelectionCriteria
import akka.stream.scaladsl.Source

private[akka] abstract class AbstractSnapshotStoreDao extends SnapshotStoreDao {

  final override def fetchSnapshot(
      persistenceId: String,
      criteria: SnapshotSelectionCriteria
  ): Source[SnapshotEntry, NotUsed] =
    doFetchSnapshot(persistenceId, criteria)

  final override def save(entry: SnapshotEntry): Source[Int, NotUsed] =
    doSave(entry).map(_.toInt)


  final override def deleteSnapshot(persistenceId: String, seqNr: Long): Source[Int, NotUsed] =
    doDeleteSnapshot(persistenceId, seqNr).map(_.toInt)

  final override def deleteSnapshot(
      persistenceId: String,
      criteria: SnapshotSelectionCriteria
  ): Source[Int, NotUsed] =
    doDeleteSnapshot(persistenceId, criteria).map(_.toInt)

  def doFetchSnapshot(
      persistenceId: String,
      criteria: SnapshotSelectionCriteria
  ): Source[SnapshotEntry, NotUsed]

  def doSave(entry: SnapshotEntry): Source[Integer, NotUsed]

  def doDeleteSnapshot(persistenceId: String, seqNr: JLong): Source[Integer, NotUsed]

  def doDeleteSnapshot(persistenceId: String, criteria: SnapshotSelectionCriteria): Source[Integer, NotUsed]

}
