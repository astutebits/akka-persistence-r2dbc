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
import io.r2dbc.spi.test._
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers
import reactor.core.publisher.Mono

class HandleSpec extends AnyFlatSpecLike with Matchers with BeforeAndAfterAll {

  private implicit val system: ActorSystem = ActorSystem()
  private implicit val mat: Materializer = Materializer(system)

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "FluxHandle" should "commit successful transaction" in {
    val connection = MockConnection.empty

    val source = Source.fromPublisher(
      new Handle(connection).inTransaction[Int](_ => Mono.just(100))
    )

    source.runWith(TestSink.probe[Int])
        .requestNext(100)
        .expectComplete()

    connection.isBeginTransactionCalled shouldBe true
    connection.isCommitTransactionCalled shouldBe true
  }

  it should "rollback erroneous transaction" in {
    val connection = MockConnection.empty
    val exception = new IllegalArgumentException("Boom")

    val source = Source.fromPublisher(
      new Handle(connection).inTransaction[Int](_ => Mono.error(exception))
    )

    source.runWith(TestSink.probe[Int])
        .expectSubscriptionAndError(exception)

    connection.isBeginTransactionCalled shouldBe true
    connection.isRollbackTransactionCalled shouldBe true
  }

  it should "execute query" in {
    val metadata = MockRowMetadata.builder()
        .columnMetadata(MockColumnMetadata.builder()
            .name("id").nativeTypeMetadata(100)
            .build())
        .build()
    val row1 = MockRow.builder.identified("id", classOf[Integer], 1).build
    val row2 = MockRow.builder.identified("id", classOf[Integer], 2).build
    val result = MockResult.builder()
        .rowMetadata(metadata)
        .row(row1, row2)
        .build()
    val statement = MockStatement.builder.result(result).build()
    val connection = MockConnection.builder.statement(statement).build

    val source = Source.fromPublisher(
      new Handle(connection).executeQuery[Integer](
        "SELECT id FROM table",
        result => result.map((row, _) => row.get("id", classOf[Integer]))
      )
    )

    source.runWith(TestSink.probe[Integer])
        .requestNext(1)
        .requestNext(2)
        .expectComplete()

    connection.getCreateStatementSql shouldBe "SELECT id FROM table"
  }

}
