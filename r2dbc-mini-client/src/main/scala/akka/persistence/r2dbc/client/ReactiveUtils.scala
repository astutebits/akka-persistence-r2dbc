/*
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

package akka.persistence.r2dbc.client

import org.reactivestreams.Publisher
import reactor.core.publisher.{Flux, Mono}
import java.util.function

/**
 * Utilities for working with Reactive flows.
 */
private[client] object ReactiveUtils {

  /**
   * Execute the [[Publisher]] provided by the given supplier and propagate the error that
   * initiated this behavior. Typically used with [[Flux#onErrorResume(Function)]] and
   * [[Mono#onErrorResume(Function)]].
   *
   * @param fn a supplier of a [[Publisher]] to execute when an error occurs
   * @tparam T the type passing through the flow
   * @return a [[Mono#error(Throwable)]] with the original error
   * @see [[Flux#onErrorResume(Function)]]
   * @see [[Mono#onErrorResume(Function)]]
   */
  def appendError[T](fn: () => Publisher[_]): function.Function[_ >: Throwable, _ <: Mono[_ <: T]] = {
    require(fn != null, "s must not be null")
    throwable => Flux.from(fn()).`then`(Mono.error[T](throwable))
  }

  /**
   * Converts a `Publisher[Void]` to a `Publisher[T]` allowing for type pass through behavior.
   *
   * @param fn a supplier of a [[Publisher]] to execute
   * @tparam T the type passing through the flow
   * @return [[Mono#empty()]] of the appropriate type
   */
  def passThrough[T](fn: () => Publisher[_]): Mono[T] = {
    require(fn != null, "s must not be null")
    Flux.from(fn()).`then`(Mono.empty[T])
  }

}
