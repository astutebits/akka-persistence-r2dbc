package akka.persistence.postgresql.query

import akka.persistence.query.{AllPersistenceIdSpec, EventsByPersistenceIdSpec, EventsByTagSpec, ReadJournalSpec}
import akka.persistence.r2dbc.client.R2dbc
import com.typesafe.config.ConfigFactory
import io.r2dbc.postgresql.{PostgresqlConnectionConfiguration, PostgresqlConnectionFactory}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.Eventually
import scala.concurrent.duration._

object PostgreSqlReadJournalSpec {

  private val Config = ConfigFactory.parseString(
    """
      |akka.persistence.journal.plugin = "postgresql-journal"
      |akka.persistence.publish-plugin-commands = on
      |akka.actor.allow-java-serialization = on
      |""".stripMargin)

}
final class PostgreSqlReadJournalSpec
    extends ReadJournalSpec(PostgreSqlReadJournalSpec.Config)
        with AllPersistenceIdSpec
        with EventsByTagSpec
        with EventsByPersistenceIdSpec
        with Eventually
        with BeforeAndAfterEach {

  private val r2dbc = new R2dbc(
    new PostgresqlConnectionFactory(PostgresqlConnectionConfiguration.builder()
        .host("localhost")
        .username("postgres")
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

  override def pluginId: String = "postgresql-read-journal"

}
