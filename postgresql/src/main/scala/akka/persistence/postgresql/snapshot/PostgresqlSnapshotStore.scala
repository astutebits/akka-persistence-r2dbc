package akka.persistence.postgresql.snapshot

import akka.actor.ActorSystem
import akka.persistence.r2dbc.client.R2dbc
import akka.persistence.r2dbc.snapshot.{ReactiveSnapshotStore, SnapshotStoreDao}
import com.typesafe.config.Config
import io.r2dbc.postgresql.{PostgresqlConnectionConfiguration, PostgresqlConnectionFactory}

final class PostgresqlSnapshotStore(config: Config) extends ReactiveSnapshotStore {

  private val storeConfig = SnapshotStoreConfig(config)
  private val factory = new PostgresqlConnectionFactory(PostgresqlConnectionConfiguration.builder()
      .host(storeConfig.hostname)
      .username(storeConfig.username)
      .password(storeConfig.password)
      .database(storeConfig.database)
      .build())

  override protected val system: ActorSystem = context.system

  override protected val dao: SnapshotStoreDao = new PostgresqlSnapshotStoreDao(new R2dbc(factory))

}
