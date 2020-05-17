package akka.persistence.postgresql.journal

import akka.persistence.CapabilityFlag
import akka.persistence.journal.JournalSpec
import com.typesafe.config.ConfigFactory
import io.r2dbc.postgresql.{PostgresqlConnectionConfiguration, PostgresqlConnectionFactory}
import org.scalatest.concurrent.Eventually
import reactor.core.publisher.{Flux, Mono}
import scala.concurrent.duration._

object PostgresqlJournalSpec {

  private val JournalPluginConfig = ConfigFactory.parseString(
    """
      |akka.persistence.journal.plugin = "postgres-journal"
      |postgres-journal.class = "akka.persistence.postgresql.journal.PostgresqlJournal"
      |akka.loglevel = "DEBUG"
      |""".stripMargin)
}

/**
 * Test case for [[PostgresqlJournal]].
 */
final class PostgresqlJournalSpec
    extends JournalSpec(config = PostgresqlJournalSpec.JournalPluginConfig)
        with Eventually {

  override def beforeAll(): Unit = {
    eventually(timeout(60.seconds), interval(1.second)) {
      val cf = new PostgresqlConnectionFactory(PostgresqlConnectionConfiguration.builder()
          .host("localhost")
          .username("postgres")
          .password("s3cr3t")
          .database("db")
          .build())
      cf.create.flatMapMany(connection => {
        connection.createBatch()
            .add("DELETE FROM journal_event")
            .add("DELETE FROM tag")
            .execute().flatMap(_.getRowsUpdated())
            .concatWith(Flux.from(connection.close).`then`(Mono.empty()))
            .onErrorResume(ex => Flux.from(connection.close).`then`(Mono.error(ex)))
      })
          .blockLast()
    }

    super.beforeAll()
  }

  override protected def supportsRejectingNonSerializableObjects: CapabilityFlag = true

}
