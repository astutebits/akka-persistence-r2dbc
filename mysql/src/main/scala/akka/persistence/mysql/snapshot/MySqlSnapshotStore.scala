package akka.persistence.mysql.snapshot

import akka.actor.ActorSystem
import akka.persistence.r2dbc.client.R2dbc
import akka.persistence.r2dbc.snapshot.{ReactiveSnapshotStore, SnapshotStoreDao}
import com.typesafe.config.Config
import dev.miku.r2dbc.mysql.{MySqlConnectionConfiguration, MySqlConnectionFactory}

/**
 * An implementation of the `Snapshot store plugin API` with `r2dbc-mysql`.
 *
 * @see [[https://doc.akka.io/docs/akka/current/persistence-journals.html#snapshot-store-plugin-api Snapshot store plugin API]]
 * @see [[https://github.com/mirromutth/r2dbc-mysql r2dbc-mysql]]
 */
final class MySqlSnapshotStore(config: Config) extends ReactiveSnapshotStore {

  private val storeConfig = MySqlSnapshotStoreConfig(config)
  private val factory = MySqlConnectionFactory.from(
    MySqlConnectionConfiguration.builder()
        .host(storeConfig.hostname)
        .username(storeConfig.username)
        .password(storeConfig.password)
        .database(storeConfig.database)
        .build()
  )

  override protected val system: ActorSystem = context.system
  override protected val dao: SnapshotStoreDao = new MySqlSnapshotStoreDao(new R2dbc(factory))

}
