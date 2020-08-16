package akka.persistence.r2dbc.snapshot

import akka.actor.ActorSystem
import akka.persistence.{SelectedSnapshot, SnapshotMetadata, SnapshotSelectionCriteria}
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}

import scala.concurrent.{ExecutionContextExecutor, Future}

private[akka] trait SnapshotLogic {

  protected val system: ActorSystem
  protected val serializer: SnapshotSerializer
  protected val dao: SnapshotStoreDao

  private lazy implicit val mat: Materializer = Materializer(system)
  private lazy implicit val ec: ExecutionContextExecutor = system.dispatcher


  def loadAsync(
      persistenceId: String,
      criteria: SnapshotSelectionCriteria
  ): Future[Option[SelectedSnapshot]] = {
    dao.fetchSnapshot(persistenceId, criteria)
        .map(entry => serializer.deserialize(entry))
        .map(_.get)
        .runWith(Sink.lastOption)
  }

  def saveAsync(metadata: SnapshotMetadata, snapshot: Any): Future[Unit] = {
    Source.future(Future.fromTry(serializer.serialize(metadata, snapshot)))
        .flatMapConcat(dao.save)
        .runWith(Sink.last)
        .map(_ => ())
  }

  def deleteAsync(metadata: SnapshotMetadata): Future[Unit] = {
    dao.deleteSnapshot(metadata.persistenceId, metadata.sequenceNr)
        .runWith(Sink.last)
        .map(_ => ())
  }

  def deleteAsync(persistenceId: String, criteria: SnapshotSelectionCriteria): Future[Unit] = {
    dao.deleteSnapshot(persistenceId, criteria)
        .runWith(Sink.last)
        .map(_ => ())
  }

}
