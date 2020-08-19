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

package akka.persistence.r2dbc.client

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.stream.testkit.scaladsl.TestSink
import akka.testkit.TestKit
import io.r2dbc.spi.test.{MockConnection, MockConnectionFactory}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers
import reactor.core.publisher.{Flux, Mono}

final class R2dbcSpec extends AnyFlatSpecLike with Matchers with BeforeAndAfterAll {

  private implicit val system: ActorSystem = ActorSystem()
  private implicit val mat: Materializer = Materializer(system)

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "R2dbc" should "open and close the connection if successful withHandle execution" in {
    val connection = MockConnection.empty
    val connectionFactory = MockConnectionFactory.builder.connection(connection).build
    val r2dbc = new R2dbc(connectionFactory)

    Source.fromPublisher[Int](r2dbc.withHandle(_ => Flux.just(1, 2)))
        .runWith(TestSink.probe[Int])
        .requestNext(1)
        .requestNext(2)
        .expectComplete()

    connection.isCloseCalled shouldBe true
  }

  it should "open and close the connection if erroneous withHandle execution" in {
    val connection = MockConnection.empty
    val connectionFactory = MockConnectionFactory.builder.connection(connection).build
    val r2dbc = new R2dbc(connectionFactory)

    Source.fromPublisher[Int](r2dbc.withHandle(_ => Mono.error(new IllegalArgumentException("Boom"))))
        .runWith(TestSink.probe[Int])
        .expectSubscriptionAndError()

    connection.isCloseCalled shouldBe true
  }

  it should "open and close the connection if successful inTransaction execution" in {
    val connection = MockConnection.empty
    val connectionFactory = MockConnectionFactory.builder.connection(connection).build
    val r2dbc = new R2dbc(connectionFactory)

    Source.fromPublisher[Int](r2dbc.inTransaction(_ => Flux.just(1, 2)))
        .runWith(TestSink.probe[Int])
        .requestNext(1)
        .requestNext(2)
        .expectComplete()

    connection.isBeginTransactionCalled shouldBe true
    connection.isCommitTransactionCalled shouldBe true
    connection.isCloseCalled shouldBe true
  }

  it should "open and close the connection if erroneous inTransaction execution" in {
    val connection = MockConnection.empty
    val connectionFactory = MockConnectionFactory.builder.connection(connection).build
    val r2dbc = new R2dbc(connectionFactory)

    Source.fromPublisher[Int](r2dbc.inTransaction(_ => Mono.error(new IllegalArgumentException("Boom"))))
        .runWith(TestSink.probe[Int])
        .expectSubscriptionAndError()

    connection.isBeginTransactionCalled shouldBe true
    connection.isRollbackTransactionCalled shouldBe true
    connection.isCloseCalled shouldBe true
  }

}