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

  private val r2dbc = new R2dbc(
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
