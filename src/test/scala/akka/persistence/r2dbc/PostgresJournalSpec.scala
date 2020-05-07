package akka.persistence.r2dbc

import akka.persistence.CapabilityFlag
import akka.persistence.journal.JournalSpec
import com.typesafe.config.ConfigFactory
import io.r2dbc.postgresql.{PostgresqlConnectionConfiguration, PostgresqlConnectionFactory}
import org.scalatest.concurrent.Eventually

import scala.concurrent.duration._

object PostgresJournalSpec {
  val PluginConfig = ConfigFactory.parseString(
    """
      |akka.persistence.journal.plugin = "postgres-journal"
      |""".stripMargin)
}

class PostgresJournalSpec
  extends JournalSpec(config = PostgresJournalSpec.PluginConfig)
    with Eventually {

  override def beforeAll(): Unit = {
    eventually(timeout(60.seconds), interval(1.second)) {
      val cf = new PostgresqlConnectionFactory(PostgresqlConnectionConfiguration.builder()
        .host("localhost")
        .username("postgres")
        .password("s3cr3t")
        .database("db")
        .build())
      val r2dbc = cf.create

      r2dbc.flatMapMany(connection => {
        connection.createBatch()
          .add("DELETE FROM journal_event")
          .add("DELETE FROM tag")
          .execute().flatMap(_.getRowsUpdated())
      }).blockFirst()

    }

    super.beforeAll()
  }

  override protected def supportsRejectingNonSerializableObjects: CapabilityFlag = true

}
