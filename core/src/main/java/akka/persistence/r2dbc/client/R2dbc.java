package akka.persistence.r2dbc.client;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import java.util.Objects;
import java.util.function.Function;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * A basic implementation of a Reactive Relational Database Connection Client.
 */
public final class R2dbc {

  private static final String FACTORY_REQUIRED = "factory must not be null";
  private static final String FN_REQUIRED = "fn must not be null";

  private final ConnectionFactory factory;

  /**
   * Create a new instance of {@link R2dbc}.
   *
   * @param factory a {@link ConnectionFactory} used to create {@link Connection}s when required
   * @throws NullPointerException if {@code factory} is {@code null}
   */
  public R2dbc(ConnectionFactory factory) {
    this.factory = Objects.requireNonNull(factory, FACTORY_REQUIRED);
  }

  /**
   * Execute behavior within a transaction returning results.  The transaction is committed if the
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
    return withHandle(handle -> handle.inTransaction(fn));
  }

  /**
   * Execute behavior with a {@link Handle} returning results.
   *
   * @param fn a {@link Function} that takes a {@link Handle} and returns a {@link Publisher} of
   * results
   * @param <T> the type of results
   * @return a {@link Flux} of results
   * @throws IllegalArgumentException if {@code resourceFunction} is {@code null}
   */
  public <T> Flux<T> withHandle(Function<Handle, ? extends Publisher<T>> fn) {
    Objects.requireNonNull(fn, FN_REQUIRED);
    return Mono.from(factory.create())
        .map(Handle::new)
        .flatMapMany(handle -> Flux.from(fn.apply(handle))
            .concatWith(ReactiveUtils.typeSafe(handle::close))
            .onErrorResume(ReactiveUtils.appendError(handle::close))
        );
  }

}
