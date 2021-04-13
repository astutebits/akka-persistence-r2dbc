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

package akka.persistence.r2dbc.query

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.stream.testkit.scaladsl.TestSink
import akka.testkit.TestKit
import org.mockito.scalatest.ResetMocksAfterEachTest
import org.mockito.{ ArgumentMatchersSugar, IdiomaticMockito }
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers
import scala.collection.immutable.Seq
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

  implicit private val system: ActorSystem = ActorSystem()
  implicit private val mat: Materializer = Materializer(system)

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
    dao.fetchPersistenceIds(0).returns(Source(Seq((1L, "foo"), (3L, "bar"))))

    Source
      .fromGraph(AllPersistentIdStage(dao))
      .runWith(TestSink.probe[String])
      .ensureSubscription()
      .requestNext("foo")
      .requestNext("bar")
      .expectComplete()
  }

  it should "fetch persistence ids indefinitely if 'refreshInterval' is specified" in {
    dao.fetchPersistenceIds(0).returns(Source(Seq((1L, "foo"), (3L, "bar"))))
    dao.fetchPersistenceIds(4).returns(Source(Seq((4L, "foo"), (5L, "bar"), (6L, "baz"))))
    dao.fetchPersistenceIds(7).returns(Source.empty)

    Source
      .fromGraph(AllPersistentIdStage(dao, Some(100.millis)))
      .runWith(TestSink.probe[String])
      .ensureSubscription()
      .requestNext("foo")
      .requestNext("bar")
      .requestNext("baz")
      .request(1)
      .expectNoMessage(300.millis)
      .cancel()

    dao.fetchPersistenceIds(0).wasCalled(once)
    dao.fetchPersistenceIds(4).wasCalled(once)
    dao.fetchPersistenceIds(7).wasCalled(atLeastTwice)
  }

}
