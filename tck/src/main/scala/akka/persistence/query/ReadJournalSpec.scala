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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers
import scala.annotation.tailrec

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

  private val actorInstanceId = 1

  protected implicit val system: ActorSystem = ActorSystem(getClass.getSimpleName, config)
  protected implicit val mat: Materializer = Materializer(system)

  protected val readJournal: RJ = PersistenceQuery(system).readJournalFor[RJ](pluginId)
  private val senderProbe: TestProbe = TestProbe()

  private val writeUuid = UUID.randomUUID()
  private val pIdNum = new AtomicInteger(0)
  private val pIdSeqNrs = new ConcurrentHashMap[String, Int]()

  private val journal: ActorRef = Persistence(system).journalFor(null)

  def pluginId: String

  private def writeMessages(fromSnr: Int, toSnr: Int, pId: String, writerUuid: UUID, tags: Set[String] = Set.empty): Unit = {

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

    val msgs = (fromSnr until toSnr).map(i => AtomicWrite(persistentRepr(i)))
    journal ! WriteMessages(msgs, senderProbe.ref, actorInstanceId)

    senderProbe.expectMsg(WriteMessagesSuccessful)
    (fromSnr until toSnr).foreach { i =>
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

  /**
   * Returns a new unique persistence ID.
   */
  protected def newPersistenceId: String = {
    val pId = s"p-${pIdNum.incrementAndGet}"
    pIdSeqNrs.putIfAbsent(pId, 1)
    pId
  }

  /**
   * Returns the last persistence ID.
   */
  protected def getPersistenceId: String = s"p-${pIdNum.get}"

  /**
   * Returns a list of all persistence IDs created so far.
   */
  protected def getAllPersistenceIds: List[String] = {
    if (pIdNum.get() == 0) return List.empty
    for (i <- (1 to pIdNum.get).toList) yield s"p-$i"
  }

  /**
   * Persists a `numOfEvents` for a `persistenceId` with optional `tags`.
   */
  @tailrec
  protected final def persist(persistenceId: String, numOfEvents: Int, tags: Set[String] = Set.empty): String = {
    val oldSeq = pIdSeqNrs.get(persistenceId)
    val newSeq = oldSeq + numOfEvents
    if (pIdSeqNrs.replace(persistenceId, oldSeq, newSeq)) {
      writeMessages(oldSeq, newSeq, persistenceId, writeUuid, tags)
      persistenceId
    }
    else persist(persistenceId, numOfEvents)
  }

}
