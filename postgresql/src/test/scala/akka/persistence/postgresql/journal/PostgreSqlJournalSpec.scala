package akka.persistence.postgresql.journal

import akka.persistence.CapabilityFlag
import akka.persistence.journal.JournalSpec
import com.typesafe.config.ConfigFactory
import io.r2dbc.postgresql.{PostgresqlConnectionConfiguration, PostgresqlConnectionFactory}
import org.scalatest.concurrent.Eventually
import reactor.core.publisher.{Flux, Mono}
import reactor.util.Loggers
import scala.concurrent.duration._

object PostgreSqlJournalSpec {

  private val JournalPluginConfig = ConfigFactory.parseString(
    """
      |akka.persistence.journal.plugin = "postgresql-journal"
      |akka.loglevel = "DEBUG"
      |akka.loggers = ["akka.event.slf4j.Slf4jLogger"]
      |akka.logging-filter = "akka.event.slf4j.Slf4jLoggingFilter"
      |""".stripMargin)
}

/**
 * Test case for [[PostgreSqlJournal]].
 */
final class PostgreSqlJournalSpec
    extends JournalSpec(config = PostgreSqlJournalSpec.JournalPluginConfig)
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
            .add("DELETE FROM event")
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
