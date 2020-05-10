package akka.persistence.r2dbc.client

import akka.NotUsed
import akka.stream.scaladsl.{Concat, Source}
import io.r2dbc.spi.{Connection, Result}
import org.reactivestreams.Publisher

/**
 * A wrapper for a [[Connection]], converting from `Reactor` to `Akka Streams` and providing
 * convenience APIs.
 */
final class Handle(connection: Connection) {
  require(connection != null, "connection cannot be null")

  /**
   * Executes the given SQL statement, and transforms each [[Result]]s that are returned from execution.
   *
   * @param sql SQL statement
   * @param f a function used to transform each [[Result]] into a `Publisher` of values
   * @tparam T the type of results
   * @return the values resulting from the [[Result]] transformation
   */
  def executeQuery[T](sql: String, f: Result => Publisher[T]): Source[T, NotUsed] =
    Source.fromPublisher(connection.createStatement(sql).execute())
        .flatMapConcat(result => Source.fromPublisher(f(result)))

  /**
   * Execute behavior within a transaction returning results. The transaction is committed if
   * the behavior completes successfully, and rolled back if it produces an error.
   *
   * @param f a function that takes a [[Handle]] and returns a [[Source]] of results
   * @tparam T the type of results
   * @return a [[Source]] of results
   */
  def inTransaction[T](f: Handle => Source[T, NotUsed]): Source[T, NotUsed] =
    Source.combine(fromVoidPublisher(connection.beginTransaction), f(this))(Concat(_))
        .flatMapConcat(result => fromVoidPublisher(connection.commitTransaction)
            // The concatenation below triggers the commit of the transaction
            .concat(Source.single(result))
        )
        .recoverWithRetries(attempts = 1, {
          case throwable: Throwable => fromVoidPublisher(connection.rollbackTransaction)
              // The concatenation below triggers the roll back the transaction
              .concat(Source.failed(throwable))
        })

}
