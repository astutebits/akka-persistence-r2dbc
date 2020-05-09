package akka.persistence.r2dbc.snapshot

import akka.persistence.snapshot.SnapshotStore
import akka.persistence.{SelectedSnapshot, SnapshotMetadata, SnapshotSelectionCriteria}
import akka.serialization.SerializationExtension
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import com.typesafe.config.Config
import io.r2dbc.postgresql.{PostgresqlConnectionConfiguration, PostgresqlConnectionFactory}
import scala.concurrent.{ExecutionContext, Future}

class PostgresSnapshotStore(config: Config) extends SnapshotStore {

  private implicit val mat: Materializer = Materializer(context.system)
  private implicit val ec: ExecutionContext = context.dispatcher

  private val storeConfig = SnapshotStoreConfig(config)
  private val serializer = new SnapshotSerializer(SerializationExtension(context.system))
  private val cf = new PostgresqlConnectionFactory(PostgresqlConnectionConfiguration.builder()
      .host(storeConfig.hostname)
      .username(storeConfig.username)
      .password(storeConfig.password)
      .database(storeConfig.database)
      .build())
  private val dao = new PostgresSnapshotStoreDao(cf)

  override def loadAsync(persistenceId: String, criteria: SnapshotSelectionCriteria): Future[Option[SelectedSnapshot]] = {
    Source.fromPublisher(dao.fetchSnapshot(persistenceId, criteria))
        .map(entry => serializer.deserialize(entry))
        .map(snapshot => snapshot.getOrElse(throw new IllegalArgumentException))
        .runWith(Sink.headOption)
  }

  override def saveAsync(metadata: SnapshotMetadata, snapshot: Any): Future[Unit] = {
    Source.future(Future.fromTry(serializer.serialize(metadata, snapshot)))
        .flatMapConcat(entry => Source.fromPublisher(dao.save(entry)))
        .map(_ => ())
        .runWith(Sink.last)
  }

  override def deleteAsync(metadata: SnapshotMetadata): Future[Unit] = {
    Source.fromPublisher(dao.deleteSnapshot(metadata.persistenceId, metadata.sequenceNr))
        .map(_ => ())
        .runWith(Sink.last)
  }

  override def deleteAsync(persistenceId: String, criteria: SnapshotSelectionCriteria): Future[Unit] = {
    Source.fromPublisher(dao.deleteSnapshots(persistenceId, criteria))
        .map(_ => ())
        .runWith(Sink.last)
  }

}
