/*
 * Copyright 2020-2021 Borislav Borisov.
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

package akka.persistence.r2dbc.journal

import akka.actor.ActorSystem
import akka.persistence.journal.Tagged
import akka.persistence.{ AtomicWrite, PersistentRepr }
import akka.stream.scaladsl.Source
import akka.testkit.TestKit
import com.typesafe.config.ConfigFactory
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger
import org.mockito.ArgumentMatchersSugar
import org.mockito.scalatest.{ AsyncIdiomaticMockito, ResetMocksAfterEachAsyncTest }
import org.scalatest.flatspec.AsyncFlatSpecLike
import org.scalatest.matchers.should.Matchers
import org.scalatest.{ BeforeAndAfterAll, TryValues }
import scala.collection.immutable.Seq
import scala.concurrent.Await
import scala.concurrent.duration.{ Duration, DurationInt }

final class ReactiveJournalSpec
    extends AsyncFlatSpecLike
    with AsyncIdiomaticMockito
    with ArgumentMatchersSugar
    with ResetMocksAfterEachAsyncTest
    with Matchers
    with TryValues
    with BeforeAndAfterAll {

  private val _system = ActorSystem(
    getClass.getSimpleName,
    ConfigFactory
      .parseString("""
          |akka.actor.allow-java-serialization = on
          |akka.persistence.journal.plugin = "fake"
          |fake.replay-messages-timeout = 100ms
      """.stripMargin)
      .withFallback(ConfigFactory.defaultApplication())
      .resolve())

  private val mockDao = mock[JournalDao]

  private val journal = new JournalLogic {
    override protected val dao: JournalDao = mockDao
    implicit override val system: ActorSystem = _system
  }

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(_system)
    super.afterAll()
  }

  class Test

  "ReactiveJournal" should "write messages and return Nil when all successfully serialized" in {
    val writes = Seq(
      AtomicWrite(
        Seq(
          PersistentRepr(Tagged("foo", Set("FooTag", "FooTagTwo")), 1L, "foo"),
          PersistentRepr(Tagged("foo2", Set("FooTag", "FooTagTwo")), 2L, "foo"))),
      AtomicWrite(PersistentRepr(Tagged("bar", Set("BarTag")), 2L, "bar")))
    mockDao.writeEvents(anySeq).returns(Source(Seq(10)))

    journal.asyncWriteMessages(writes).map { result =>
      mockDao.writeEvents(anySeq).was(called)

      result shouldBe Nil
    }
  }

  it should "only write serialized messages and return a Seq marking the failed ones" in {
    val writes = Seq(
      AtomicWrite(
        Seq(
          PersistentRepr(Tagged("foo", Set("FooTag", "FooTagTwo")), 1L, "foo"),
          PersistentRepr(Tagged("foo2", Set("FooTag", "FooTagTwo")), 2L, "foo"))),
      AtomicWrite(PersistentRepr(new Test, 1, "test")),
      AtomicWrite(PersistentRepr(Tagged("bar", Set("BarTag")), 2L, "bar")))

    mockDao.writeEvents(anySeq).returns(Source(Seq(10)))

    journal.asyncWriteMessages(writes).map { result =>
      mockDao.writeEvents(anySeq).was(called)

      result.size shouldBe 3
      (result.head should be).a(Symbol("success"))
      (result(1) should be).a(Symbol("failure"))
      (result.last should be).a(Symbol("success"))
    }
  }

  it should "fail the future if it cannot write the messages" in {
    val writes = Seq(
      AtomicWrite(
        Seq(
          PersistentRepr(Tagged("foo", Set("FooTag", "FooTagTwo")), 1L, "foo"),
          PersistentRepr(Tagged("foo2", Set("FooTag", "FooTagTwo")), 2L, "foo"))),
      AtomicWrite(PersistentRepr(new Test, 1, "test")),
      AtomicWrite(PersistentRepr(Tagged("bar", Set("BarTag")), 2L, "bar")))

    val exception = new IllegalArgumentException("Boom")
    mockDao.writeEvents(anySeq).returns(Source.failed(exception))

    recoverToExceptionIf[IllegalArgumentException] {
      journal.asyncWriteMessages(writes)
    }.map { result =>
      mockDao.writeEvents(anySeq).was(called)
      result shouldBe exception
    }
  }

  it should "return Success if the removal of the messages was successful" in {
    mockDao.deleteEvents("foo", 100).returns(Source.single(100))

    journal.asyncDeleteMessagesTo("foo", 100).map { result =>
      mockDao.deleteEvents("foo", 100).was(called)
      result shouldBe ((): Unit)
    }
  }

  it should "return Failure if the removal of the messages failed" in {
    val exception = new IllegalArgumentException("Boom")
    mockDao.deleteEvents("foo", 100).returns(Source.failed(exception))

    recoverToExceptionIf[IllegalArgumentException] {
      journal.asyncDeleteMessagesTo("foo", 100)
    }.map { result =>
      mockDao.deleteEvents("foo", 100).was(called)
      result shouldBe exception
    }
  }

  it should "replay messages as requested" in {
    mockDao
      .fetchEvents("foo", 1, 2, 2)
      .returns(Source(Seq(
        Await
          .result(
            journal.reprSerializer.serialize(PersistentRepr(Tagged("foo", Set("FooTag", "FooTagTwo")), 1L, "foo")),
            Duration.Inf)
          .map(_.copy(id = 1L)),
        Await
          .result(
            journal.reprSerializer.serialize(PersistentRepr(Tagged("foo", Set("FooTag", "FooTagTwo")), 2L, "foo")),
            Duration.Inf)
          .map(_.copy(id = 5L)))).map(_.get))

    val counter = new AtomicInteger(0)
    journal.asyncReplayMessages("foo", 1, 2, 2)(_ => counter.incrementAndGet()).map { result =>
      mockDao.fetchEvents("foo", 1, 2, 2).was(called)
      counter.get shouldBe 2

      result shouldBe ((): Unit)
    }
  }

  it should "return Failure if it cannot replay the messages due to a DB error" in {
    val exception = new IllegalArgumentException("Boom")
    mockDao.fetchEvents("foo", 1, 2, 2).returns(Source.failed(exception))

    recoverToExceptionIf[IllegalArgumentException] {
      journal.asyncReplayMessages("foo", 1, 2, 2)(println)
    }.map { result =>
      mockDao.fetchEvents("foo", 1, 2, 2).was(called)

      result shouldBe exception
    }
  }

  it should "return Failure if the query times out" in {
    mockDao
      .fetchEvents("foo", 1, 2, 2)
      .returns(
        Source(
          Seq(
            Await
              .result(
                journal.reprSerializer.serialize(PersistentRepr(Tagged("foo", Set("FooTag", "FooTagTwo")), 1L, "foo")),
                Duration.Inf)
              .map(_.copy(id = 1L)))).initialDelay(200.millis).map(_.get))

    recoverToExceptionIf[TimeoutException] {
      journal.asyncReplayMessages("foo", 1, 2, 2)(println)
    }.map { result =>
      mockDao.fetchEvents("foo", 1, 2, 2).was(called)

      result.getMessage shouldBe "The stream has not been completed in 100000000 nanoseconds."
    }
  }

  it should "return the highest sequence number" in {
    mockDao.readHighestSequenceNr("foo", 1).returns(Source(Seq(1, 2)))

    journal.asyncReadHighestSequenceNr("foo", 1).map { result =>
      mockDao.readHighestSequenceNr("foo", 1).was(called)
      result shouldBe 2
    }
  }

  it should "return 0 if the highest sequence number is nowhere to be found" in {
    mockDao.readHighestSequenceNr("foo", 1).returns(Source.empty)

    journal.asyncReadHighestSequenceNr("foo", 1).map { result =>
      mockDao.readHighestSequenceNr("foo", 1).was(called)
      result shouldBe 0
    }
  }

  it should "return Failure if the highest sequence number request fails due to a DB error" in {
    val exception = new IllegalArgumentException("Boom")
    mockDao.readHighestSequenceNr("foo", 1).returns(Source.failed(exception))

    recoverToExceptionIf[IllegalArgumentException] {
      journal.asyncReadHighestSequenceNr("foo", 1)
    }.map { result =>
      mockDao.readHighestSequenceNr("foo", 1).was(called)

      result shouldBe exception
    }
  }

}
