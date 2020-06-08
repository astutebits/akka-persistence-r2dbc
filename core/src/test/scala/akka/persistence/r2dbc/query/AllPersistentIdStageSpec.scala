package akka.persistence.r2dbc.query

import akka.actor.ActorSystem
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

/**
 * Test case for [[AllPersistentIdStage]].
 */
final class AllPersistentIdStageSpec
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

  "AllPersistenceIdStage" should "throw an exception if 'dao' is not provided" in {
    a[IllegalArgumentException] should be thrownBy {
      AllPersistentIdStage(null)
    }
  }

  it should "fetch the current persistence ids if 'refreshInterval' is not specified" in {
    dao.fetchPersistenceIds(0) returns Source(Seq((1L, "foo"), (3L, "bar")))

    Source.fromGraph(AllPersistentIdStage(dao))
        .runWith(TestSink.probe[String])
        .ensureSubscription()
        .requestNext("foo")
        .requestNext("bar")
        .expectComplete()
  }

  it should "fetch persistence ids indefinitely if 'refreshInterval' is specified" in {
    dao.fetchPersistenceIds(0) returns Source(Seq((1L, "foo"), (3L, "bar")))
    dao.fetchPersistenceIds(4) returns Source(Seq((4L, "foo"), (5L, "bar"), (6L, "baz")))
    dao.fetchPersistenceIds(7) returns Source.empty

    Source.fromGraph(AllPersistentIdStage(dao, Some(100.millis)))
        .runWith(TestSink.probe[String])
        .ensureSubscription()
        .requestNext("foo")
        .requestNext("bar")
        .requestNext("baz")
        .request(1)
        .expectNoMessage(300.millis)
        .cancel()

    dao.fetchPersistenceIds(0) wasCalled once
    dao.fetchPersistenceIds(4) wasCalled once
    dao.fetchPersistenceIds(7) wasCalled atLeastTwice
  }

}
