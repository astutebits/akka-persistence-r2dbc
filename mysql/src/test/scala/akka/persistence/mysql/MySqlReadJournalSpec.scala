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

package akka.persistence.mysql

import akka.persistence.query.{AllPersistenceIdSpec, EventsByPersistenceIdSpec, EventsByTagSpec, ReadJournalSpec}
import akka.persistence.r2dbc.client.R2dbc
import com.typesafe.config.ConfigFactory
import dev.miku.r2dbc.mysql.{MySqlConnectionConfiguration, MySqlConnectionFactory}
import org.scalatest.concurrent.Eventually
import scala.concurrent.duration._

object MySqlReadJournalSpec {

  private val Config = ConfigFactory.parseString(
    """
      |akka.persistence.journal.plugin = "mysql-journal"
      |akka.persistence.publish-plugin-commands = on
      |akka.actor.allow-java-serialization = on
      |""".stripMargin)

}
final class MySqlReadJournalSpec
    extends ReadJournalSpec(MySqlReadJournalSpec.Config)
        with AllPersistenceIdSpec
        with EventsByTagSpec
        with EventsByPersistenceIdSpec
        with Eventually {

  private val r2dbc = R2dbc(
    MySqlConnectionFactory.from(MySqlConnectionConfiguration.builder()
        .host("localhost")
        .username("root")
        .password("s3cr3t")
        .database("db")
        .build())
  )

  override def beforeAll(): Unit = {
    eventually(timeout(60.seconds), interval(1.second)) {
      r2dbc.withHandle(_.executeQuery("DELETE FROM event; DELETE FROM tag;", _.getRowsUpdated))
          .blockLast()
    }

    super.beforeAll()
  }

  override def pluginId: String = "mysql-read-journal"

}
