package akka.persistence.mysql.snapshot

import akka.actor.ActorSystem
import akka.persistence.r2dbc.ConnectionPoolFactory
import akka.persistence.r2dbc.client.R2dbc
import akka.persistence.r2dbc.snapshot.{ReactiveSnapshotStore, SnapshotStoreDao, SnapshotStorePluginConfig}
import com.typesafe.config.Config

/**
 * An implementation of the `Snapshot store plugin API` with `r2dbc-mysql`.
 *
 * @see [[https://doc.akka.io/docs/akka/current/persistence-journals.html#snapshot-store-plugin-api Snapshot store plugin API]]
 * @see [[https://github.com/mirromutth/r2dbc-mysql r2dbc-mysql]]
 */
private[akka] final class MySqlSnapshotStore(config: Config) extends ReactiveSnapshotStore {

  override protected val system: ActorSystem = context.system
  override protected val dao: SnapshotStoreDao =
    new MySqlSnapshotStoreDao(new R2dbc(ConnectionPoolFactory("mysql", SnapshotStorePluginConfig(config))))

}
