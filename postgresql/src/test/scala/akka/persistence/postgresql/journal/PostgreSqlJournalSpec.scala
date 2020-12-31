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

package akka.persistence.postgresql.journal

import akka.persistence.CapabilityFlag
import akka.persistence.journal.JournalSpec
import akka.persistence.r2dbc.client.R2dbc
import com.typesafe.config.ConfigFactory
import io.r2dbc.postgresql.{PostgresqlConnectionConfiguration, PostgresqlConnectionFactory}
import org.scalatest.concurrent.Eventually
import scala.concurrent.duration._

object PostgreSqlJournalSpec {

  private val JournalPluginConfig = ConfigFactory.parseString(
    """
      |akka.persistence.journal.plugin = "postgresql-journal"
      |akka.loglevel = "DEBUG"
      |""".stripMargin)
}

/**
 * Test case for [[PostgreSqlJournal]].
 */
final class PostgreSqlJournalSpec
    extends JournalSpec(config = PostgreSqlJournalSpec.JournalPluginConfig)
        with Eventually with ProjectionExtensionSpec {

  protected val r2dbc: R2dbc = R2dbc(
    new PostgresqlConnectionFactory(PostgresqlConnectionConfiguration.builder()
        .host("localhost")
        .username("postgres")
        .password("s3cr3t")
        .database("db")
        .build())
  )

  override def beforeAll(): Unit = {
    eventually(timeout(60.seconds), interval(1.second)) {
      r2dbc.withHandle(handle => handle.executeQuery("TRUNCATE event, tag; " +
          "CREATE TABLE IF NOT EXISTS projected (id varchar(255), value text);", _.getRowsUpdated
      )).blockLast()
    }

    super.beforeAll()
  }

  override protected def supportsRejectingNonSerializableObjects: CapabilityFlag = true

}
