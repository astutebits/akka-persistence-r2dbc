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

import scala.collection.immutable.Seq
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success, Try }

private[akka] object TryUtil {

  /**
   * Flatten a collection of try's to give either a success of the try values, or just the failure.
   *
   * @param seq the collection
   * @tparam A value's type
   * @return success of the try values or failure
   */
  def flatten[A](seq: Seq[Try[A]]): Try[Seq[A]] = {
    @scala.annotation.tailrec
    def go(remaining: Seq[Try[A]], processed: Seq[A]): Try[Seq[A]] = remaining match {
      case Seq()                 => Success(processed)
      case Success(head) +: tail => go(remaining = tail, processed :+ head)
      case Failure(t) +: _       => Failure(t)
    }

    go(seq, Vector.empty)
  }

  /**
   * Creates the signal for a successful write as per the Journal API specification:
   *
   * The journal can also signal that it rejects individual messages (`AtomicWrite`) by the
   * returned `immutable.Seq[Try[Unit]]`. It is possible but not mandatory to reduce number of
   * allocations by returning `Nil` for the happy path, i.e. when no messages are rejected.
   * Otherwise the returned `Seq` must have as many elements as the input `messages` `Seq`.
   * Each `Try` element signals if the corresponding `AtomicWrite` is rejected or not,
   * with an exception describing the problem.
   *
   * @param xs the serialized writes
   * @tparam A the internal AtomicWrite serialized type
   * @return Nil if all writes serialization was successful, otherwise a `Seq` with `Try`s
   */
  def writeCompleteSignal[A](xs: Seq[Try[A]]): Seq[Try[Unit]] =
    if (xs.forall(_.isSuccess)) Nil else xs.map(_.map(_ => ()))

  /**
   * Wraps a `Future`'s outcome in a `Try`.
   *
   * @param future the future to wrap
   * @param ec an `ExecutionContext`
   * @tparam T the future type
   * @return the wrapped outcome
   */
  def futureTry[T](future: () => Future[T])(implicit ec: ExecutionContext): Future[Try[T]] =
    future().map(Success(_)).recover { case e => Failure(e) }

}
