package akka.persistence.postgresql.journal

import scala.collection.immutable.Nil
import scala.util.{Failure, Success, Try}

private[akka] object TrySeq {

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
      case Seq() =>
        Success(processed)
      case Success(head) +: tail =>
        go(remaining = tail, processed :+ head)
      case Failure(t) +: _ =>
        Failure(t)
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

}
