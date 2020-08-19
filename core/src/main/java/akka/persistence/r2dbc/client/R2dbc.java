/*
 * Copyright 2017-2019 the original author or authors.
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
            .concatWith(ReactiveUtils.passThrough(handle::close))
            .onErrorResume(ReactiveUtils.appendError(handle::close))
        );
  }

}
