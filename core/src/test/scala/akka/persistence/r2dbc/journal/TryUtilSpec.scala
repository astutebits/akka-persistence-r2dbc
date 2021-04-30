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

import org.scalatest.TryValues._
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers
import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, Future }
import scala.util.{ Failure, Success }

/**
 * Test case for [[TryUtil]].
 */
final class TryUtilSpec extends AnyFlatSpecLike with Matchers {

  private val iae = new IllegalArgumentException("Poof")
  private val ise = new IllegalStateException("Boom")

  "TryUtil.flatten" should "flatten Seq[Success[T]] to Success[Seq[T]]" in {
    TryUtil.flatten(Seq(Success(1))) shouldBe Success(Seq(1))
    TryUtil.flatten(Seq(Success(1), Success(2))) shouldBe Success(Seq(1, 2))
  }

  it should "flatten Seq[Failure[T]] to Failure[Seq[T]]" in {
    val onlyFailure = TryUtil.flatten(Seq(Failure(iae)))
    val multipleFailures = TryUtil.flatten(Seq(Failure(ise), Failure(iae)))
    val mixed = TryUtil.flatten(Seq(Success(1), Failure(iae)))
    val anotherMix = TryUtil.flatten(Seq(Failure(iae), Success(1)))

    (onlyFailure should be).a(Symbol("failure"))
    onlyFailure.failure.exception shouldBe iae
    (multipleFailures should be).a(Symbol("failure"))
    multipleFailures.failure.exception shouldBe ise
    (mixed should be).a(Symbol("failure"))
    mixed.failure.exception shouldBe iae
    (anotherMix should be).a(Symbol("failure"))
    anotherMix.failure.exception shouldBe iae
  }

  "TryUtil.writeCompleteSignal" should "create the 'write complete signal' from the serialized events Seq[Try[T]]" in {
    val allSuccessfullySerialized = Seq(Success(1), Success(2), Success(3))
    val partiallySerialized = Seq(Success(1), Failure(iae), Success(3))

    TryUtil.writeCompleteSignal(allSuccessfullySerialized) shouldBe Nil
    TryUtil.writeCompleteSignal(partiallySerialized) shouldBe Seq(Success(()), Failure(iae), Success(()))
  }

  "TryUtil.futureTry" should "wrap a successful Future as Future[Success[T]]" in {
    val future = TryUtil.futureTry(() => Future.successful("Nice, eh?"))
    Await.result(future, Duration.Inf) shouldBe Success("Nice, eh?")
  }

  it should "wrap a failed Future as Future[Failure[T]]" in {
    val future = TryUtil.futureTry(() => Future.failed(ise))
    Await.result(future, Duration.Inf) shouldBe Failure(ise)
  }

}
