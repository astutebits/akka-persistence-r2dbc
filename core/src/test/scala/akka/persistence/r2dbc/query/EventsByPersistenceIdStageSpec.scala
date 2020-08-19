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

package akka.persistence.r2dbc.query

import akka.actor.ActorSystem
import akka.persistence.r2dbc.journal.JournalEntry
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.stream.testkit.scaladsl.TestSink
import akka.testkit.TestKit
import org.mockito.scalatest.ResetMocksAfterEachTest
import org.mockito.{ArgumentMatchersSugar, IdiomaticMockito}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers
import scala.concurrent.duration._
import scala.util.Random

/**
 * Test case for [[EventsByPersistenceIdStage]].
 */
final class EventsByPersistenceIdStageSpec
    extends AnyFlatSpecLike
        with IdiomaticMockito
        with ResetMocksAfterEachTest
        with Matchers
        with ArgumentMatchersSugar
        with BeforeAndAfterAll {

  private implicit val system: ActorSystem = ActorSystem()
  private implicit val mat: Materializer = Materializer(system)

  private val dao: QueryDao = mock[QueryDao]

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
    super.afterAll()
  }

  "EventsByPersistenceIdStage" should "throw an exception when 'dao' is not provided" in {
    a[IllegalArgumentException] should be thrownBy {
      EventsByPersistenceIdStage(null, "persistenceId", 0, Long.MaxValue)
    }
  }

  it should "throw an exception when 'persistenceId' is not provided" in {
    a[IllegalArgumentException] should be thrownBy {
      EventsByPersistenceIdStage(dao, "", 0, Long.MaxValue)
    }

    a[IllegalArgumentException] should be thrownBy {
      EventsByPersistenceIdStage(dao, null, 0, Long.MaxValue)
    }
  }

  it should "throw an exception when 'fromSeqNr' is negative" in {
    val r = new Random()
    for {
      _ <- 0 to 10
    } yield {
      a[IllegalArgumentException] should be thrownBy {
        EventsByPersistenceIdStage(dao, "foo", r.between(Long.MinValue, 0), Long.MaxValue)
      }
    }
  }

  it should "throw an exception when 'toSeqNr' is negative" in {
    val r = new Random()
    for {
      _ <- 0 to 10
    } yield {
      a[IllegalArgumentException] should be thrownBy {
        EventsByPersistenceIdStage(dao, "foo", 0, r.between(Long.MinValue, 0))
      }
    }
  }

  it should "thrown and exception if 'fromSeqNr' > 'toSeqNr'" in {
    a[IllegalArgumentException] should be thrownBy {
      EventsByPersistenceIdStage(dao, "foo", 1, 0)
    }
  }

  it should "fetch the current events if 'refreshInterval' is not specified and the subset is" in {
    val pId = "foo"
    val events = List(
      JournalEntry(1, pId, 1, "test-value".getBytes, tags = Set("FooEvent")),
      JournalEntry(3, pId, 2, "test-value-2".getBytes, tags = Set("FooEvent")),
      JournalEntry(5, pId, 3, "test-value-3".getBytes, tags = Set("FooEvent"))
    )

    dao.findHighestSeq(pId) returns Source.single(3)
    dao.fetchByPersistenceId(pId, 1, 3) returns Source(events)

    Source.fromGraph(EventsByPersistenceIdStage(dao, pId, 1, 3))
        .runWith(TestSink.probe[JournalEntry])
        .ensureSubscription()
        .requestNext(events.head)
        .requestNext(events(1))
        .requestNext(events.last)
        .expectComplete()
  }

  it should "pick the real toSeqNr regardless of the user input when fetching current events" in {
    val pId = "foo"
    val events = List(
      JournalEntry(1, pId, 1, "test-value".getBytes, tags = Set("FooEvent")),
      JournalEntry(3, pId, 2, "test-value-2".getBytes, tags = Set("FooEvent")),
      JournalEntry(5, pId, 3, "test-value-3".getBytes, tags = Set("FooEvent"))
    )

    dao.findHighestSeq(pId) returns Source.single(3)
    dao.fetchByPersistenceId(pId, 0, 3) returns Source(events)

    Source.fromGraph(EventsByPersistenceIdStage(dao, pId, 0, Long.MaxValue))
        .runWith(TestSink.probe[JournalEntry])
        .ensureSubscription()
        .requestNext(events.head)
        .requestNext(events(1))
        .requestNext(events.last)
        .expectComplete()
  }

  it should "fetch events until the given subset is returned if 'refreshInterval' is defined" in {
    val pId = "foo"
    val firstSet = Seq(
      JournalEntry(1, pId, 1, "test-value".getBytes, tags = Set("FooEvent")),
      JournalEntry(3, pId, 2, "test-value-2".getBytes, tags = Set("FooEvent")),
      JournalEntry(5, pId, 3, "test-value-3".getBytes, tags = Set("FooEvent"))
    )
    val secondSet = Seq(
      JournalEntry(7, pId, 4, "test-value-4".getBytes, tags = Set("FooEvent")),
      JournalEntry(9, pId, 5, "test-value-5".getBytes, tags = Set("FooEvent"))
    )

    dao.findHighestSeq(pId) returns Source.single(3) andThen Source.single(5)
    dao.fetchByPersistenceId(pId, 1, 3) returns Source(firstSet)
    dao.fetchByPersistenceId(pId, 4, 5) returns Source(secondSet)

    Source.fromGraph(EventsByPersistenceIdStage(dao, pId, 1, 5, Some(100.millis)))
        .runWith(TestSink.probe[JournalEntry])
        .ensureSubscription()
        .requestNext(firstSet.head)
        .requestNext(firstSet(1))
        .requestNext(firstSet.last)
        .requestNext(secondSet.head)
        .requestNext(secondSet.last)
        .expectComplete()
  }

  it should "fetch events indefinitely when refreshInterval is specified" in {
    val pId = "foo"
    val firstSet = Seq(
      JournalEntry(1, pId, 1, "test-value".getBytes, tags = Set("FooEvent")),
      JournalEntry(3, pId, 2, "test-value-2".getBytes, tags = Set("FooEvent")),
      JournalEntry(5, pId, 3, "test-value-3".getBytes, tags = Set("FooEvent"))
    )
    val secondSet = Seq(
      JournalEntry(7, pId, 4, "test-value-4".getBytes, tags = Set("FooEvent")),
      JournalEntry(9, pId, 5, "test-value-5".getBytes, tags = Set("FooEvent"))
    )

    dao.findHighestSeq(pId) returns Source.single(3) andThen Source.single(5)
    dao.fetchByPersistenceId(pId, 0, 3) returns Source(firstSet)
    dao.fetchByPersistenceId(pId, 4, 5) returns Source(secondSet)

    Source.fromGraph(EventsByPersistenceIdStage(dao, pId, 0, Long.MaxValue, Some(100.millis)))
        .runWith(TestSink.probe[JournalEntry])
        .ensureSubscription()
        .requestNext(firstSet.head)
        .requestNext(firstSet(1))
        .requestNext(firstSet.last)
        .requestNext(secondSet.head)
        .requestNext(secondSet.last)
        .request(1)
        .expectNoMessage(200.millis)
        .cancel()
  }

}
