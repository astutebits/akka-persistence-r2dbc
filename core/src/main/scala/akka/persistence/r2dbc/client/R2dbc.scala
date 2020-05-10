package akka.persistence.r2dbc.client

import akka.NotUsed
import akka.stream.scaladsl.Source
import io.r2dbc.spi.ConnectionFactory

/**
 * A basic implementation of a Reactive Relational Database Connection Client.
 */
final class R2dbc(factory: ConnectionFactory) {
  require(factory != null, "factory cannot be null")

  /**
   * Execute behavior within a transaction returning results. The transaction is committed if
   * the behavior completes successfully, and rolled back if it produces an error.
   *
   * @param f a function that takes a [[Handle]] and returns a [[Source]] of results
   * @tparam T the type of results
   * @return a [[Source]] of results
   */
  def inTransaction[T](f: Handle => Source[T, NotUsed]): Source[T, NotUsed] = {
    withHandle(handle => handle.inTransaction(f))
  }

  /**
   * Execute behavior with a [[Handle]] returning results.
   *
   * @param f a function that takes a [[Handle]] and returns a [[Source]] of results
   * @tparam T the type of results
   * @return a [[Source]] of results
   */
  def withHandle[T](f: Handle => Source[T, NotUsed]): Source[T, NotUsed] = {
    Source.fromPublisher(factory.create())
        .flatMapConcat(connection => f(new Handle(connection))
            .flatMapConcat(result => fromVoidPublisher(connection.close())
                // Close the connection and complete the stream with
                // the last element from the previous source
                .concat(Source.single(result)))
            .recoverWithRetries(attempts = 1, {
              case throwable: Throwable => fromVoidPublisher(connection.close)
                  // Close the connection and fail the stream
                  .concat(Source.failed(throwable))
            })
        )
  }

}
