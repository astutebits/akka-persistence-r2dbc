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

package akka.persistence.r2dbc.client

import io.r2dbc.spi.test.{MockConnection, MockConnectionFactory}
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers
import reactor.core.publisher.{Flux, Mono}
import reactor.test.StepVerifier

/**
 * Test case for [[R2dbc]].
 */
final class R2dbcSpec extends AnyFlatSpecLike with Matchers {

  "R2dbc" should "open and close the connection if successful withHandle execution" in {
    val connection = MockConnection.empty
    val connectionFactory = MockConnectionFactory.builder.connection(connection).build
    val r2dbc = R2dbc(connectionFactory)

    StepVerifier.create(r2dbc.withHandle(_ => Flux.just(1, 2)))
        .expectNext(1)
        .expectNext(2)
        .verifyComplete

    connection.isCloseCalled shouldBe true
  }

  it should "open and close the connection if erroneous withHandle execution" in {
    val connection = MockConnection.empty
    val connectionFactory = MockConnectionFactory.builder.connection(connection).build
    val r2dbc = R2dbc(connectionFactory)

    StepVerifier.create(r2dbc.withHandle(_ => Mono.error[Int](new IllegalArgumentException("BOOM!"))))
        .expectErrorMessage("BOOM!")
        .verify

    connection.isCloseCalled shouldBe true
  }

  it should "open and close the connection if successful inTransaction execution" in {
    val connection = MockConnection.empty
    val connectionFactory = MockConnectionFactory.builder.connection(connection).build
    val r2dbc = R2dbc(connectionFactory)

    StepVerifier.create(r2dbc.inTransaction(_ => Flux.just(1, 2)))
        .expectNext(1)
        .expectNext(2)
        .verifyComplete

    connection.isBeginTransactionCalled shouldBe true
    connection.isCommitTransactionCalled shouldBe true
    connection.isCloseCalled shouldBe true
  }

  it should "open and close the connection if erroneous inTransaction execution" in {
    val connection = MockConnection.empty
    val connectionFactory = MockConnectionFactory.builder.connection(connection).build
    val r2dbc = R2dbc(connectionFactory)

    StepVerifier.create(r2dbc.inTransaction(_ => Mono.error[Int](new IllegalArgumentException("BOOM!"))))
        .expectErrorMessage("BOOM!")
        .verify

    connection.isBeginTransactionCalled shouldBe true
    connection.isRollbackTransactionCalled shouldBe true
    connection.isCloseCalled shouldBe true
  }

  it should "throw exception if `connection` is null when calling apply(Connection)" in {
    val caught = intercept[IllegalArgumentException] {
      R2dbc(null)
    }
    caught.getMessage shouldBe "requirement failed: factory must not be null"
  }

  it should "throw exception if `fn` is null when calling inTransaction(Handle => Publisher)" in {
    val connection = MockConnection.empty
    val factory = MockConnectionFactory.builder.connection(connection).build
    val caught = intercept[IllegalArgumentException] {
      R2dbc(factory).inTransaction(null)
    }
    caught.getMessage shouldBe "requirement failed: fn must not be null"
  }

  it should "throw exception if `fn` is null when calling withHandle(Handle => Publisher)" in {
    val connection = MockConnection.empty
    val factory = MockConnectionFactory.builder.connection(connection).build
    val caught = intercept[IllegalArgumentException] {
      R2dbc(factory).withHandle(null)
    }
    caught.getMessage shouldBe "requirement failed: fn must not be null"
  }

}