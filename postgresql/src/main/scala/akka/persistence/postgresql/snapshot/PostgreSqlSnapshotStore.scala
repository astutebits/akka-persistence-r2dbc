package akka.persistence.postgresql.snapshot

import akka.actor.ActorSystem
import akka.persistence.r2dbc.ConnectionPoolFactory
import akka.persistence.r2dbc.client.R2dbc
import akka.persistence.r2dbc.snapshot.{ReactiveSnapshotStore, SnapshotStoreDao, SnapshotStorePluginConfig}
import com.typesafe.config.Config

private[akka] final class PostgreSqlSnapshotStore(config: Config) extends ReactiveSnapshotStore {

  override protected val system: ActorSystem = context.system
  override protected val dao: SnapshotStoreDao =
    new PostgreSqlSnapshotStoreDao(new R2dbc(ConnectionPoolFactory("postgresql", SnapshotStorePluginConfig(config))))

}
