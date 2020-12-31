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
    new MySqlSnapshotStoreDao(R2dbc(ConnectionPoolFactory("mysql", SnapshotStorePluginConfig(config))))

}
