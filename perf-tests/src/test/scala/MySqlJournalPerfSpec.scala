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

