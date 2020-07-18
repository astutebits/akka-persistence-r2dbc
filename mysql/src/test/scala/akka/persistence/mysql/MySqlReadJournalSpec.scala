package akka.persistence.mysql

import akka.persistence.query.{AllPersistenceIdSpec, EventsByPersistenceIdSpec, EventsByTagSpec, ReadJournalSpec}
import akka.persistence.r2dbc.client.R2dbc
import com.typesafe.config.ConfigFactory
import dev.miku.r2dbc.mysql.{MySqlConnectionConfiguration, MySqlConnectionFactory}
import org.scalatest.concurrent.Eventually
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import scala.concurrent.duration._

object MySqlReadJournalSpec {

  private val Config = ConfigFactory.parseString(
    """
      |akka.persistence.journal.plugin = "mysql-journal"
      |akka.persistence.publish-plugin-commands = on
      |akka.actor.allow-java-serialization = on
      |""".stripMargin)

}
class MySqlReadJournalSpec
    extends ReadJournalSpec(MySqlReadJournalSpec.Config)
        with AllPersistenceIdSpec
        with EventsByTagSpec
        with EventsByPersistenceIdSpec
        with Eventually
        with BeforeAndAfterEach {

  private val r2dbc = new R2dbc(
    MySqlConnectionFactory.from(MySqlConnectionConfiguration.builder()
        .host("localhost")
        .username("root")
        .password("s3cr3t")
        .database("db")
        .build())
  )

  override def beforeEach(): Unit = {
    eventually(timeout(60.seconds), interval(1.second)) {
      r2dbc.withHandle(handle =>
        handle.executeQuery("DELETE FROM event; DELETE FROM tag;", _.getRowsUpdated)
      )
          .blockLast()
    }

    super.beforeEach()
  }

  override def pluginId: String = "mysql-read-journal"

}
