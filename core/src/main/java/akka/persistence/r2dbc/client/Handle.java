package akka.persistence.r2dbc.client;

import static akka.persistence.r2dbc.client.ReactiveUtils.appendError;
import static akka.persistence.r2dbc.client.ReactiveUtils.passThrough;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.Result;
import java.util.Objects;
import java.util.function.Function;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * A wrapper for a {@link Connection} providing convenience APIs.
 */
public final class Handle {

  private static final String CONNECTION_REQUIRED = "factory must not be null";
  private static final String SQL_REQUIRED = "sql must not be null";
  private static final String FN_REQUIRED = "fn must not be null";

  private final Connection connection;

  Handle(Connection connection) {
    this.connection = Objects.requireNonNull(connection, CONNECTION_REQUIRED);
  }

  /**
   * Release any resources held by the {@link Handle}.
   *
   * @return a {@link Publisher} that termination is complete
   */
  Publisher<Void> close() {
    return this.connection.close();
  }

  /**
   * Execute behavior within a transaction returning results. The transaction is committed if the
   * behavior completes successfully, and rolled back it produces an error.
   *
   * @param fn a {@link Function} that takes a {@link Handle} and returns a {@link Publisher} of
   * results
   * @param <T> the type of results
   * @return a {@link Flux} of results
   * @throws NullPointerException if {@code fn} is {@code null}
   * @see Connection#commitTransaction()
   * @see Connection#rollbackTransaction()
   */
  public <T> Flux<T> inTransaction(Function<Handle, ? extends Publisher<T>> fn) {
    Objects.requireNonNull(fn, FN_REQUIRED);

    return Mono.from(beginTransaction())
        .thenMany(fn.apply(this))
        .concatWith(passThrough(this::commitTransaction))
        .onErrorResume(appendError(this::rollbackTransaction));
  }

  /**
   * Executes the given SQL statement, and transforms each {@link Result}s that are returned from
   * execution.
   *
   * @param sql SQL statement
   * @param fn a function used to transform each {@link Result} into a {@link Publisher} of values
   * @param <T> the type of results
   * @return the values resulting from the {@link Result} transformation
   * @throws NullPointerException if {@code sql} or {@code fn} is {@code null}
   */
  public <T> Flux<T> executeQuery(String sql, Function<Result, ? extends Publisher<T>> fn) {
    Objects.requireNonNull(sql, SQL_REQUIRED);
    Objects.requireNonNull(fn, FN_REQUIRED);
    return Flux.from(this.connection.createStatement(sql).execute()).flatMap(fn);
  }

  /**
   * Commits the current transaction.
   *
   * @return a {@link Publisher} that indicates that a transaction has been committed
   */
  private Publisher<Void> commitTransaction() {
    return this.connection.commitTransaction();
  }

  /**
   * Begins a new transaction.
   *
   * @return a {@link Publisher} that indicates that the transaction is open
   */
  private Publisher<Void> beginTransaction() {
    return this.connection.beginTransaction();
  }

  /**
   * Rolls back the current transaction.
   *
   * @return a {@link Publisher} that indicates that a transaction has been rolled back
   */
  private Publisher<Void> rollbackTransaction() {
    return this.connection.rollbackTransaction();
  }

}
