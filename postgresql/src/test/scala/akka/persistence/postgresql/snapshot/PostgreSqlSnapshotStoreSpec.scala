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

package akka.persistence.postgresql.snapshot

import akka.persistence.CapabilityFlag
import akka.persistence.snapshot.SnapshotStoreSpec
import com.typesafe.config.ConfigFactory
import io.r2dbc.postgresql.{PostgresqlConnectionConfiguration, PostgresqlConnectionFactory}
import org.scalatest.concurrent.Eventually
import reactor.core.publisher.{Flux, Mono}
import java.time.{Duration => JDuration}

import scala.concurrent.duration._


object PostgreSqlSnapshotStoreSpec {
  private val PluginConfig = ConfigFactory.parseString(
    """
      |akka.persistence.snapshot-store.plugin = "postgresql-snapshot-store"
      |""".stripMargin)

}

/**
 * Test case for [[PostgreSqlSnapshotStore]].
 */
final class PostgreSqlSnapshotStoreSpec
    extends SnapshotStoreSpec(config = PostgreSqlSnapshotStoreSpec.PluginConfig)
        with Eventually {

  override def beforeAll(): Unit = {
    eventually(timeout(60.seconds), interval(1.second)) {
      val cf = new PostgresqlConnectionFactory(PostgresqlConnectionConfiguration.builder()
          .host("localhost")
          .username("postgres")
          .password("s3cr3t")
          .database("db")
          .port(5433)
          .connectTimeout(JDuration.ofSeconds(3))
          .build())

      cf.create.flatMapMany(connection => {
        connection.createBatch()
            .add("DELETE FROM snapshot")
            .execute().flatMap(_.getRowsUpdated())
            .concatWith(Flux.from(connection.close).`then`(Mono.empty()))
            .onErrorResume(ex => Flux.from(connection.close).`then`(Mono.error(ex)))
      })
          .blockLast()
    }

    super.beforeAll()
  }

  override def supportsSerialization: CapabilityFlag = true

}
