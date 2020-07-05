package akka.persistence.query

import akka.actor.{ActorRef, ActorSystem}
import akka.persistence.JournalProtocol.{WriteMessageSuccess, WriteMessages, WriteMessagesSuccessful}
import akka.persistence.journal.Tagged
import akka.persistence.query.scaladsl._
import akka.persistence.{AtomicWrite, Persistence, PersistentImpl, PersistentRepr}
import akka.stream.Materializer
import akka.testkit.{TestKit, TestProbe}
import com.typesafe.config.Config
import java.util.UUID
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

abstract class ReadJournalSpec(config: Config)
    extends AnyFlatSpecLike
        with Matchers
        with BeforeAndAfterAll {

  type RJ = ReadJournal
      with CurrentPersistenceIdsQuery
      with PersistenceIdsQuery
      with CurrentEventsByPersistenceIdQuery
      with EventsByPersistenceIdQuery
      with CurrentEventsByTagQuery
      with EventsByTagQuery

  protected val actorInstanceId = 1

  protected implicit val system: ActorSystem = ActorSystem(getClass.getSimpleName, config)
  protected implicit val mat: Materializer = Materializer(system)

  protected val readJournal: RJ = PersistenceQuery(system).readJournalFor[RJ](pluginId)
  private val senderProbe: TestProbe = TestProbe()

  private val journal: ActorRef = Persistence(system).journalFor(null)

  def pluginId: String

  final def writeMessages(fromSnr: Int, toSnr: Int, pId: String, writerUuid: UUID, tags: Set[String] = Set.empty): Unit = {

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

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
    super.afterAll()
  }

}
