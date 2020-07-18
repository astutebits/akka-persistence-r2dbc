package akka.persistence.postgresql.query

import akka.actor.{ActorRef, ActorSystem}
import akka.persistence.JournalProtocol.{WriteMessageSuccess, WriteMessages, WriteMessagesSuccessful}
import akka.persistence.journal.Tagged
import akka.persistence.postgresql.query.scaladsl.PostgreSqlReadJournal
import akka.persistence.query.PersistenceQuery
import akka.persistence.r2dbc.client.R2dbc
import akka.persistence.{AtomicWrite, Persistence, PersistentImpl, PersistentRepr}
import akka.stream.Materializer
import akka.testkit.{TestKit, TestProbe}
import com.typesafe.config.{Config, ConfigFactory}
import io.r2dbc.postgresql.{PostgresqlConnectionConfiguration, PostgresqlConnectionFactory}
import java.util.UUID
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.Eventually
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers
import scala.concurrent.duration._

trait PersistenceQuerySpec
    extends AnyFlatSpecLike
        with Matchers
        with Eventually
        with BeforeAndAfterAll {

  val config: Config = ConfigFactory.parseString(
    """
      |akka.persistence.journal.plugin = "postgresql-journal"
      |akka.actor.allow-java-serialization = on
      |""".stripMargin
  ).withFallback(ConfigFactory.defaultApplication()).resolve()

  protected implicit val system: ActorSystem = ActorSystem(getClass.getSimpleName, config)
  protected implicit val mat: Materializer = Materializer(system)

  protected val actorInstanceId = 1
  protected val readJournal: PostgreSqlReadJournal = PersistenceQuery(system)
      .readJournalFor[PostgreSqlReadJournal](PostgreSqlReadJournal.Identifier)

  private val senderProbe: TestProbe = TestProbe()
  private val journal: ActorRef = Persistence(system).journalFor(null)
  private val r2dbc = new R2dbc(
    new PostgresqlConnectionFactory(PostgresqlConnectionConfiguration.builder()
        .host("localhost")
        .username("postgres")
        .password("s3cr3t")
        .database("db")
        .build())
  )

  def writeMessages(fromSnr: Int, toSnr: Int, pId: String, writerUuid: UUID, tags: Set[String] = Set.empty): Unit = {

    def persistentRepr(sequenceNr: Long) =
      PersistentRepr(
        payload = tags match {
          case set if set.nonEmpty => Tagged(s"$pId-$sequenceNr", set)
          case _ => s"$pId-$sequenceNr"
        },
        sequenceNr = sequenceNr,
        persistenceId = pId,
        sender = senderProbe.ref,
        writerUuid = writerUuid.toString)

    val msgs = (fromSnr to toSnr).map(i => AtomicWrite(persistentRepr(i)))
    journal ! WriteMessages(msgs, senderProbe.ref, actorInstanceId)

    senderProbe.expectMsg(WriteMessagesSuccessful)
    (fromSnr to toSnr).foreach { i =>
      senderProbe.expectMsgPF() {
        case WriteMessageSuccess(PersistentImpl(payload, `i`, `pId`, _, _, _, _, _), _) =>
          tags match {
            case set if set.nonEmpty => payload should be(Tagged(s"$pId-$i", set))
            case _ => payload should be(s"$pId-$i")
          }
      }
    }
  }

  override def beforeAll(): Unit = {
    deleteEvents()
    super.beforeAll()
  }

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
    super.afterAll()
  }

  protected def deleteEvents(): Unit = {
    eventually(timeout(60.seconds), interval(1.second)) {
      r2dbc.withHandle(handle => handle.executeQuery(
        "DELETE FROM event; DELETE FROM tag;", _.getRowsUpdated()
      ))
          .blockLast()
    }
  }

}
