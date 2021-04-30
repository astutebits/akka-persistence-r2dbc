/*
 * Scala (https://www.scala-lang.org)
 *
 * Copyright EPFL and Lightbend, Inc.
 *
 * All of the code below is taken from the 2.13 scala.util.Random.
 */
package akka.persistence.r2dbc

import scala.annotation.tailrec
import scala.util.Random

package object query {
  val Random = new Random()

  /**
   * Returns a pseudorandom, uniformly distributed long value between 0
   *  (inclusive) and the specified value (exclusive), drawn from this
   *  random number generator's sequence.
   */
  def nextLong(n: Long): Long = {
    require(n > 0, "n must be positive")

    var offset = 0L
    var _n = n

    while (_n >= Integer.MAX_VALUE) {
      val bits = Random.nextInt(2)
      val halfn = _n >>> 1
      val nextn =
        if ((bits & 2) == 0) halfn
        else _n - halfn
      if ((bits & 1) == 0)
        offset += _n - nextn
      _n = nextn
    }
    offset + Random.nextInt(_n.toInt)
  }

  /**
   * Returns a pseudorandom, uniformly distributed long value between min
   *  (inclusive) and the specified value max (exclusive), drawn from this
   *  random number generator's sequence.
   */
  def randomBetween(minInclusive: Long, maxExclusive: Long): Long = {
    require(minInclusive < maxExclusive, "Invalid bounds")

    val difference = maxExclusive - minInclusive
    if (difference >= 0) {
      nextLong(difference) + minInclusive
    } else {
      /* The interval size here is greater than Long.MaxValue,
       * so the loop will exit with a probability of at least 1/2.
       */
      @tailrec
      def loop(): Long = {
        val n = Random.nextLong()
        if (n >= minInclusive && n < maxExclusive) n
        else loop()
      }
      loop()
    }
  }

}
