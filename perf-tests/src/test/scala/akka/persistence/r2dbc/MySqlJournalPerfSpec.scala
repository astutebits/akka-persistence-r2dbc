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

package akka.persistence.r2dbc

import akka.persistence.CapabilityFlag
import akka.persistence.journal.JournalPerfSpec
import akka.persistence.r2dbc.client.R2dbc
import com.typesafe.config.{Config, ConfigFactory}
import dev.miku.r2dbc.mysql.{MySqlConnectionConfiguration, MySqlConnectionFactory}
import org.scalatest.concurrent.Eventually
import scala.concurrent.duration._

final class MySqlJournalPerfSpec
    extends JournalPerfSpec(config = MySqlJournalPerfSpec.PluginConfig)
        with Eventually {

  override def beforeAll(): Unit = {
    eventually(timeout(60.seconds), interval(1.second)) {
      val r2dbc = R2dbc(
        MySqlConnectionFactory.from(MySqlConnectionConfiguration.builder()
            .host("localhost")
            .username("root")
            .password("s3cr3t")
            .database("db")
            .build())
      )
      r2dbc.withHandle(handle =>
        handle.executeQuery("DELETE FROM event; DELETE FROM tag;", _.getRowsUpdated)
      )
          .blockLast()
    }

    super.beforeAll()
  }

  override def eventsCount: Int = 1000

  override def awaitDurationMillis: Long = 1.minutes.toMillis

  override protected def supportsRejectingNonSerializableObjects: CapabilityFlag = true

}

object MySqlJournalPerfSpec {
  private val PluginConfig: Config = ConfigFactory.parseString(
    """
      |akka.persistence.journal.plugin = "mysql-journal"
      |""".stripMargin)
}

