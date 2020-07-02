package akka.persistence.mysql.journal

import akka.persistence.CapabilityFlag
import akka.persistence.journal.JournalSpec
import akka.persistence.r2dbc.client.R2dbc
import com.typesafe.config.ConfigFactory
import dev.miku.r2dbc.mysql.{MySqlConnectionConfiguration, MySqlConnectionFactory}
import org.scalatest.concurrent.Eventually
import scala.concurrent.duration._

object MySqlJournalSpec {
  private val JournalPluginConfig = ConfigFactory.parseString(
    """
      |akka.persistence.journal.plugin = "mysql-journal"
      |akka.loglevel = "INFO"
      |""".stripMargin
  )
}
final class MySqlJournalSpec
    extends JournalSpec(config = MySqlJournalSpec.JournalPluginConfig) with Eventually {

  override def beforeAll(): Unit = {
    eventually(timeout(60.seconds), interval(1.second)) {
      val r2dbc = new R2dbc(
        MySqlConnectionFactory.from(MySqlConnectionConfiguration.builder()
            .host("localhost")
            .username("root")
            .password("s3cr3t")
            .database("db")
            .build())
      )
      r2dbc.withHandle(handle =>
        handle.executeQuery("DELETE FROM journal_event; DELETE FROM event_tag;", _.getRowsUpdated)
      )
          .blockLast()
    }

    super.beforeAll()
  }

  override protected def supportsRejectingNonSerializableObjects: CapabilityFlag = true

}
