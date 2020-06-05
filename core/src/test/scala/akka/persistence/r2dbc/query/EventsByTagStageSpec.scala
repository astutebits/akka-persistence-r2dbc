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
 * Test case for [[EventsByTagStage]].
 */
final class EventsByTagStageSpec
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

  "EventsByTagStage" should "throw an exception when 'tag' is not provided" in {
    a[IllegalArgumentException] should be thrownBy {
      EventsByTagStage(dao, "", 1, Some(100.millis))
    }
    a[IllegalArgumentException] should be thrownBy {
      EventsByTagStage(dao, null, 1, Some(100.millis))
    }
  }

  it should "throw an exception when 'offset' is negative" in {
    val r = new Random()
    for {
      _ <- 0 to 10
    } yield {
      a[IllegalArgumentException] should be thrownBy {
        EventsByTagStage(dao, "tag", r.between(Int.MinValue, 0), Some(100.millis))
      }
    }
  }

  it should "fetch the current events when refreshInterval is not specified" in {
    val events = List(
      JournalEntry(1, false, "foo", 1, "test-value".getBytes, Set("FooEvent")),
      JournalEntry(3, false, "foo", 2, "test-value-2".getBytes, Set("FooEvent")),
      JournalEntry(5, false, "foo", 3, "test-value-3".getBytes, Set("FooEvent"))
    )

    dao.findHighestIndex("FooEvent") returns Source.single(5)
    dao.fetchByTag("FooEvent", 1, 5) returns Source(events)

    Source.fromGraph(EventsByTagStage(dao, "FooEvent", 1, None))
        .runWith(TestSink.probe[JournalEntry])
        .ensureSubscription()
        .requestNext(events.head)
        .requestNext(events(1))
        .requestNext(events.last)
        .expectComplete()
  }

  it should "fetch events indefinitely when refreshInterval is specified" in {
    val firstSet = Seq(
      JournalEntry(1, false, "foo", 1, "test-value".getBytes, Set("FooEvent")),
      JournalEntry(3, false, "foo", 2, "test-value-2".getBytes, Set("FooEvent")),
      JournalEntry(5, false, "foo", 3, "test-value-3".getBytes, Set("FooEvent"))
    )
    val secondSet = Seq(
      JournalEntry(7, false, "foo", 4, "test-value-4".getBytes, Set("FooEvent"))
    )

    dao.findHighestIndex("FooEvent") returns Source.single(5) andThen Source.single(7)
    dao.fetchByTag("FooEvent", 1, 5) returns Source(firstSet)
    dao.fetchByTag("FooEvent", 5, 7) returns Source(secondSet)

    Source.fromGraph(EventsByTagStage(dao, "FooEvent", 1, Some(100.millis)))
        .runWith(TestSink.probe[JournalEntry])
        .ensureSubscription()
        .requestNext(firstSet.head)
        .requestNext(firstSet(1))
        .requestNext(firstSet.last)
        .requestNext(secondSet.head)
        .expectNoMessage(200.millis)
        .cancel()

    dao.fetchByTag("FooEvent", 1, 5) was called
    dao.fetchByTag("FooEvent", 5, 7) was called
    dao.findHighestIndex("FooEvent") wasCalled atLeastThreeTimes
  }

  it should "fail the stage if the 'findHighestIndex' DAO call fails" in {
    val firstSet = Seq(
      JournalEntry(1, false, "foo", 1, "test-value".getBytes, Set("FooEvent")),
      JournalEntry(3, false, "foo", 2, "test-value-2".getBytes, Set("FooEvent")),
      JournalEntry(5, false, "foo", 3, "test-value-3".getBytes, Set("FooEvent"))
    )

    dao.findHighestIndex("FooEvent") returns Source.single(5) andThen Source.failed(new IllegalStateException("Boom"))
    dao.fetchByTag("FooEvent", 1, 5) returns Source(firstSet)

    Source.fromGraph(EventsByTagStage(dao, "FooEvent", 1, Some(100.millis)))
        .runWith(TestSink.probe[JournalEntry])
        .ensureSubscription()
        .requestNext(firstSet.head)
        .requestNext(firstSet(1))
        .requestNext(firstSet.last)
        .request(1)
        .expectError()

    dao.fetchByTag("FooEvent", 1, 5) was called
    dao.findHighestIndex("FooEvent") wasCalled twice
  }

  it should "fail the stage if the 'fetchByTag' DAO call fails" in {
    val firstSet = Seq(
      JournalEntry(1, false, "foo", 1, "test-value".getBytes, Set("FooEvent")),
      JournalEntry(3, false, "foo", 2, "test-value-2".getBytes, Set("FooEvent")),
      JournalEntry(5, false, "foo", 3, "test-value-3".getBytes, Set("FooEvent"))
    )

    dao.findHighestIndex("FooEvent") returns Source.single(5) andThen Source.single(7)
    dao.fetchByTag("FooEvent", 1, 5) returns Source(firstSet)
    dao.fetchByTag("FooEvent", 5, 7) returns Source.failed(new IllegalStateException("Boom"))

    Source.fromGraph(EventsByTagStage(dao, "FooEvent", 1, Some(100.millis)))
        .runWith(TestSink.probe[JournalEntry])
        .ensureSubscription()
        .requestNext(firstSet.head)
        .requestNext(firstSet(1))
        .requestNext(firstSet.last)
        .request(1)
        .expectError()

    dao.fetchByTag("FooEvent", 1, 5) was called
    dao.fetchByTag("FooEvent", 5, 7) was called
    dao.findHighestIndex("FooEvent") wasCalled twice
  }

}