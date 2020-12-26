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

import io.r2dbc.spi.test._
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

/**
 * Test case for [[Handle]].
 */
final class HandleSpec extends AnyFlatSpecLike with Matchers {

  "Handle" should "commit successful transaction" in {
    val connection = MockConnection.empty

    StepVerifier.create(new Handle(connection).inTransaction(_ => Mono.just(100)))
        .expectNext(100)
        .verifyComplete()

    connection.isBeginTransactionCalled shouldBe true
    connection.isCommitTransactionCalled shouldBe true
  }

  it should "rollback erroneous transaction" in {
    val connection = MockConnection.empty
    val exception = new IllegalArgumentException("Boom")

    StepVerifier.create(new Handle(connection).inTransaction[Int](_ => Mono.error(exception)))
        .expectErrorMessage(exception.getMessage)
        .verify()

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

    StepVerifier.create(new Handle(connection).executeQuery[Integer]("SELECT id FROM table",
      result => result.map((row, _) => row.get("id", classOf[Integer]))))
        .expectNext(1)
        .expectNext(2)
        .verifyComplete()

    connection.getCreateStatementSql shouldBe "SELECT id FROM table"
  }

}
