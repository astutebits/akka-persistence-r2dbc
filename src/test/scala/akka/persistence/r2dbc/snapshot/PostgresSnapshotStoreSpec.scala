package akka.persistence.r2dbc.snapshot

import akka.persistence.CapabilityFlag
import akka.persistence.snapshot.SnapshotStoreSpec
import com.typesafe.config.ConfigFactory
import io.r2dbc.postgresql.{PostgresqlConnectionConfiguration, PostgresqlConnectionFactory}
import org.scalatest.concurrent.Eventually
import reactor.core.publisher.{Flux, Mono}
import scala.concurrent.duration._

object PostgresSnapshotStoreSpec {
  private val PluginConfig = ConfigFactory.parseString(
    """
      |akka.persistence.snapshot-store.plugin = "postgres-snapshot"
      |""".stripMargin)

}

final class PostgresSnapshotStoreSpec
    extends SnapshotStoreSpec(config = PostgresSnapshotStoreSpec.PluginConfig)
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
            .add("DELETE FROM snapshot")
            .execute().flatMap(_.getRowsUpdated())
            .concatWith(Flux.from(connection.close).`then`(Mono.empty()))
            .onErrorResume(ex => Flux.from(connection.close().`then`(Mono.error(ex))))
      })
          .blockFirst()
    }

    super.beforeAll()
  }

  override def supportsSerialization: CapabilityFlag = true

}
