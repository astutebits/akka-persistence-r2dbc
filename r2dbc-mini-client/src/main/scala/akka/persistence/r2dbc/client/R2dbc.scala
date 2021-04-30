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

package akka.persistence.r2dbc.client

import akka.persistence.r2dbc.client.ReactiveUtils.{ appendError, passThrough }
import io.r2dbc.spi.{ Connection, ConnectionFactory }
import org.reactivestreams.Publisher
import reactor.core.publisher.{ Flux, Mono }

object R2dbc {

  private val FACTORY_REQUIRED: String = "factory must not be null"
  private val FN_REQUIRED: String = "fn must not be null"

  /**
   * Creates a new instance of [[R2dbc]].
   *
   * @param factory a [[ConnectionFactory]] used to create [[Connection]]s when required
   * @throws NullPointerException if `factory` is `null`
   */
  def apply(factory: ConnectionFactory): R2dbc = {
    require(factory != null, FACTORY_REQUIRED)
    new R2dbc(factory)
  }

}

/**
 * A basic implementation of a Reactive Relational Database Connection Client.
 */
final class R2dbc private (val factory: ConnectionFactory) {

  import R2dbc.FN_REQUIRED

  /**
   * Execute behavior within a transaction returning results. The transaction is committed if the
   * behavior completes successfully, and rolled back it produces an error.
   *
   * @param fn a function that takes a [[Handle]] and returns a [[Flux]] of results
   * @tparam T the type of results
   * @return a [[Flux]] of results
   * @throws NullPointerException if `fn` is `null`
   * @see [[Connection#commitTransaction()]]
   * @see [[Connection#rollbackTransaction()]]
   */
  def inTransaction[T](fn: Handle => _ <: Publisher[T]): Flux[T] = {
    require(fn != null, FN_REQUIRED)
    withHandle((handle: Handle) => handle.inTransaction(fn))
  }

  /**
   * Execute behavior with a [[Handle]] returning results.
   *
   * @param fn a function that takes a [[Handle]] and returns a [[Flux]] of results
   * @tparam T the type of results
   * @return a [[Flux]] of results
   * @throws IllegalArgumentException if `fn` is `null`
   */
  def withHandle[T](fn: Handle => _ <: Publisher[T]): Flux[T] = {
    require(fn != null, FN_REQUIRED)
    Mono
      .from(factory.create)
      .flatMap((it: Connection) => Mono.just(Handle(it)))
      .flatMapMany((handle: Handle) =>
        Flux
          .from(fn.apply(handle))
          .concatWith(passThrough(() => handle.close))
          .onErrorResume(appendError(() => handle.close)))
  }

}
