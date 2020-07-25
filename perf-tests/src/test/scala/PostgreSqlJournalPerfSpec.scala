import akka.persistence.CapabilityFlag
import akka.persistence.journal.JournalPerfSpec
import com.typesafe.config.{Config, ConfigFactory}
import io.r2dbc.postgresql.{PostgresqlConnectionConfiguration, PostgresqlConnectionFactory}
import org.scalatest.concurrent.Eventually
import reactor.core.publisher.{Flux, Mono}
import scala.concurrent.duration._

final class PostgreSqlJournalPerfSpec
    extends JournalPerfSpec(config = PostgreSqlJournalPerfSpec.PluginConfig)
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

  override def eventsCount: Int = 1000

  override def awaitDurationMillis: Long = 1.minutes.toMillis

  override protected def supportsRejectingNonSerializableObjects: CapabilityFlag = true

}
object PostgreSqlJournalPerfSpec {
  val PluginConfig: Config = ConfigFactory.parseString(
    """
      |akka.persistence.journal.plugin = "postgresql-journal"
      |""".stripMargin)
}
