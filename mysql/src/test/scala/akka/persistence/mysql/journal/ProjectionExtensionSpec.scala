/*
 * Copyright 2020 Borislav Borisov.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package akka.persistence.mysql.journal

import akka.actor.Actor
import akka.persistence.JournalProtocol._
import akka.persistence.journal.JournalSpec
import akka.persistence.r2dbc.client.R2dbc
import akka.persistence.r2dbc.journal.Projected
import akka.persistence.{AtomicWrite, PersistentImpl, PersistentRepr}
import akka.testkit.TestProbe
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import org.scalatest.wordspec.AnyWordSpecLike
import scala.annotation.tailrec

trait ProjectionExtensionSpec extends AnyWordSpecLike {
  _: JournalSpec =>

  protected val r2dbc: R2dbc

  private val pIdNum = new AtomicInteger(0)
  private val pIdSeqNrs = new ConcurrentHashMap[String, Int]()
  private val senderProbe: TestProbe = TestProbe()

  /**
   * Returns a new unique persistence ID.
   */
  protected def newPersistenceId: String = {
    val pId = s"pp-${pIdNum.incrementAndGet}"
    pIdSeqNrs.putIfAbsent(pId, 1)
    pId
  }

  protected final def persistShouldSucceed(pId: String, numOfEvents: Int, projections: Map[Int, String] = Map.empty): Unit = {
    val probe = TestProbe()
    val (fromSeqNr, toSeqNr) = persist(pId, numOfEvents, projections, probe)

    probe.expectMsg(WriteMessagesSuccessful)
    (fromSeqNr until toSeqNr).foreach { seqNr =>
      probe.expectMsgPF() {
        case WriteMessageSuccess(PersistentImpl(payload, `seqNr`, `pId`, _, _, _, _, _, _), _) =>
          projections.get(seqNr) match {
            case Some(sql) => payload shouldBe Projected(genPayload(pId, seqNr), sql)
            case None => payload shouldBe genPayload(pId, seqNr)
          }
      }
    }

    journal ! ReplayMessages(1, Long.MaxValue, Long.MaxValue, pId, probe.ref)
    (fromSeqNr until toSeqNr).foreach { seqNr =>
      probe.expectMsg(
        ReplayedMessage(PersistentImpl(genPayload(pId, seqNr), seqNr, pId, "", deleted = false, Actor.noSender, writerUuid, 0L, None))
      )
    }
  }

  protected final def persistShouldFail(pId: String, numOfEvents: Int, reason: String, projections: Map[Int, String] = Map.empty): Unit = {
    val probe = TestProbe()
    val (fromSeqNr, toSeqNr) = persist(pId, numOfEvents, projections, probe)

    probe.expectMsgPF() {
      case WriteMessagesFailed(cause, failed) =>
        cause.getMessage should include(reason)
        failed shouldBe numOfEvents
    }
    (fromSeqNr until toSeqNr).foreach { seqNr =>
      probe.expectMsgPF() {
        case WriteMessageFailure(PersistentImpl(payload, `seqNr`, `pId`, _, _, _, _, _, _), cause, _) =>
          projections.get(seqNr) match {
            case Some(sql) => payload shouldBe Projected(genPayload(pId, seqNr), sql)
            case None => payload shouldBe genPayload(pId, seqNr)
          }
          cause.getMessage should include(reason)
      }
    }

    journal ! ReplayMessages(1, Long.MaxValue, Long.MaxValue, pId, probe.ref)
    probe.expectMsg(RecoverySuccess(0))
  }

  @tailrec
  protected final def persist(
      persistenceId: String,
      numOfEvents: Int,
      projections: Map[Int, String] = Map.empty,
      probe: TestProbe = senderProbe
  ): (Int, Int) = {
    val oldSeq = pIdSeqNrs.get(persistenceId)
    val newSeq = oldSeq + numOfEvents
    if (pIdSeqNrs.replace(persistenceId, oldSeq, newSeq)) {
      writeMessages(oldSeq, newSeq, persistenceId, projections, probe)
      (oldSeq, newSeq)
    }
    else persist(persistenceId, numOfEvents)
  }

  private def genPayload(pId: String, seqNr: Int) = s"$pId-$seqNr"

  private def writeMessages(
      fromSeqNr: Int,
      toSeqNr: Int,
      pId: String,
      projections: Map[Int, String],
      probe: TestProbe
  ): Unit = {
    def persistentRepr(seqNr: Int) = PersistentRepr(
      payload = projections.get(seqNr).map(sql => Projected(genPayload(pId, seqNr), sql))
          .getOrElse(genPayload(pId, seqNr)),
      sequenceNr = seqNr,
      persistenceId = pId,
      sender = probe.ref,
      writerUuid = writerUuid
    )

    val msgs = (fromSeqNr until toSeqNr).map(i => AtomicWrite(persistentRepr(i)))
    journal ! WriteMessages(msgs, probe.ref, actorInstanceId)
  }

  "The journal projections extension" must {

    "write a single event and execute its projection" in {
      val pId = newPersistenceId
      val projections = Map(1 -> s"INSERT INTO projected (id, value) VALUES ('$pId', 'projected')")

      persistShouldSucceed(pId, 1, projections)

      val (id: String, name: String) = r2dbc.withHandle(handle => handle.executeQuery(
        s"SELECT id, value FROM projected WHERE id = '$pId';",
        _.map((row, _) => (row.get("id", classOf[String]), row.get("value", classOf[String])))
      )).blockLast()

      id shouldBe s"$pId"
      name shouldBe "projected"
    }

    "abort an event write if its projection fails" in {
      val pId = newPersistenceId
      val projections = Map(1 -> s"INSERT INTO nonexistent (id, value) VALUES ('$pId', 'projected')")
      val reason = """Table 'db.nonexistent' doesn't exist"""

      persistShouldFail(pId, projections.size, reason, projections)
    }

    "write multiple events and execute their projections" in {
      val pId = newPersistenceId
      val projections = Map(
        1 -> s"INSERT INTO projected (id, value) VALUES ('$pId', 'projected')",
        2 -> s"UPDATE projected SET value = '$pId-2' WHERE id = '$pId'",
        3 -> s"UPDATE projected SET value = '$pId-3' WHERE id = '$pId'"
      )

      persistShouldSucceed(pId, 3, projections)

      val (id: String, name: String) = r2dbc.withHandle(handle => handle.executeQuery(
        s"SELECT id, value FROM projected WHERE id = '$pId';",
        _.map((row, _) => (row.get("id", classOf[String]), row.get("value", classOf[String])))
      )).blockLast()

      id shouldBe s"$pId"
      name shouldBe s"$pId-3"
    }

    "abort all event writes when writing multiple events if any projection fails" in {
      val pId = newPersistenceId
      val projections = Map(
        1 -> s"INSERT INTO projected (id, value) VALUES ('$pId', 'projected')",
        2 -> s"UPDATE nonexistent SET value = '$pId-2' WHERE id = '$pId'",
        3 -> s"UPDATE projected SET value = '$pId-3' WHERE id = '$pId'"
      )
      val reason = """Table 'db.nonexistent' doesn't exist"""

      persistShouldFail(pId, projections.size, reason, projections)

      val rows = r2dbc.withHandle(handled => handled.executeQuery(
        s"SELECT id, value FROM projected WHERE id = '$pId';", _.getRowsUpdated
      )).blockLast()

      rows shouldBe 0
    }

    "write a mixed bunch of events with and without projections" in {
      val pId = newPersistenceId
      val projections = Map(
        1 -> s"INSERT INTO projected (id, value) VALUES ('$pId', 'projected')",
        2 -> s"UPDATE projected SET value = '$pId-2' WHERE id = '$pId'",
      )

      persistShouldSucceed(pId, 3, projections)

      val (id: String, name: String) = r2dbc.withHandle(handle => handle.executeQuery(
        s"SELECT id, value FROM projected WHERE id = '$pId';",
        _.map((row, _) => (row.get("id", classOf[String]), row.get("value", classOf[String])))
      )).blockLast()

      id shouldBe s"$pId"
      name shouldBe s"$pId-2"
    }

  }

}
