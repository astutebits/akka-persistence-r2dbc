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

package akka.persistence.mysql.snapshot

import akka.persistence.CapabilityFlag
import akka.persistence.r2dbc.client.R2dbc
import akka.persistence.snapshot.SnapshotStoreSpec
import com.typesafe.config.ConfigFactory
import dev.miku.r2dbc.mysql.{MySqlConnectionConfiguration, MySqlConnectionFactory}
import org.scalatest.concurrent.Eventually
import scala.concurrent.duration._


/**
 * Test case for [[MySqlSnapshotStore]].
 */
final class MySqlSnapshotStoreSpec
    extends SnapshotStoreSpec(config = MySqlSnapshotStoreSpec.SnapshotStoreConfig)
        with Eventually {

  override def beforeAll(): Unit = {
    eventually(timeout(60.seconds), interval(1.second)) {
      val r2dbc = R2dbc(
        MySqlConnectionFactory.from(MySqlConnectionConfiguration.builder()
            .host("docker")
            .username("root")
            .password("s3cr3t")
            .database("db")
            .build())
      )
      r2dbc.withHandle(handle => handle.executeQuery("DELETE FROM snapshot;", _.getRowsUpdated))
          .blockLast()
    }

    super.beforeAll()
  }

  override def supportsSerialization: CapabilityFlag = true

}

object MySqlSnapshotStoreSpec {
  private val SnapshotStoreConfig = ConfigFactory.parseString(
    """
      |akka.persistence.snapshot-store.plugin = "mysql-snapshot"
      |akka.loglevel = "INFO"
      |""".stripMargin
  )
}
