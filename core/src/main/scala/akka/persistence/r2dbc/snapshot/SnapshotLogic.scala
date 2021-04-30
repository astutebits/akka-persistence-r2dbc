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

import akka.actor.ActorSystem
import akka.persistence.{ SelectedSnapshot, SnapshotMetadata, SnapshotSelectionCriteria }
import akka.stream.scaladsl.{ Sink, Source }

import scala.concurrent.{ ExecutionContextExecutor, Future }

private[akka] trait SnapshotLogic {

  implicit protected val system: ActorSystem
  protected val serializer: SnapshotSerializer
  protected val dao: SnapshotStoreDao

  implicit private lazy val ec: ExecutionContextExecutor = system.dispatcher

  def loadAsync(persistenceId: String, criteria: SnapshotSelectionCriteria): Future[Option[SelectedSnapshot]] = {
    dao
      .fetchSnapshot(persistenceId, criteria)
      .map(entry => serializer.deserialize(entry))
      .map(_.get)
      .runWith(Sink.lastOption)
  }

  def saveAsync(metadata: SnapshotMetadata, snapshot: Any): Future[Unit] = {
    Source
      .future(Future.fromTry(serializer.serialize(metadata, snapshot)))
      .flatMapConcat(dao.save)
      .runWith(Sink.last)
      .map(_ => ())
  }

  def deleteAsync(metadata: SnapshotMetadata): Future[Unit] = {
    dao.deleteSnapshot(metadata.persistenceId, metadata.sequenceNr).runWith(Sink.last).map(_ => ())
  }

  def deleteAsync(persistenceId: String, criteria: SnapshotSelectionCriteria): Future[Unit] = {
    dao.deleteSnapshot(persistenceId, criteria).runWith(Sink.last).map(_ => ())
  }

}
