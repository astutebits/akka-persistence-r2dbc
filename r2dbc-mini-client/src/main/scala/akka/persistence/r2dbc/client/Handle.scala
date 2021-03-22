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

import akka.persistence.r2dbc.client.ReactiveUtils.{appendError, passThrough}
import io.r2dbc.spi.{Connection, Result}
import org.reactivestreams.Publisher
import reactor.core.publisher.{Flux, Mono}

object Handle {

  private val CONNECTION_REQUIRED: String = "connection must not be null"
  private val SQL_REQUIRED: String = "sql must not be null"
  private val FN_REQUIRED: String = "fn must not be null"

  /**
   * Creates a new instance of `Handle`.
   *
   * @param connection the wrapped [[Connection]]
   * @throws IllegalArgumentException if `connection` is `null`
   */
  def apply(connection: Connection): Handle = {
    require(connection != null, CONNECTION_REQUIRED)
    new Handle(connection)
  }

}

/**
 * A wrapper for a [[Connection]] providing convenience APIs.
 */
final class Handle private(val connection: Connection) {

  import Handle.{FN_REQUIRED, SQL_REQUIRED}

  /**
   * Execute behavior within a transaction returning results. The transaction is committed if the
   * behavior completes successfully, and rolled back it produces an error.
   *
   * @param fn a function that takes a [[Handle]] and returns a [[Publisher]] of results
   * @tparam T the type of results
   * @return a [[Flux]] of results
   * @throws IllegalArgumentException if `fn` is `null`
   * @see Connection#commitTransaction()
   * @see Connection#rollbackTransaction()
   */
  def inTransaction[T](fn: Handle => _ <: Publisher[T]): Flux[T] = {
    require(fn != null, FN_REQUIRED)
    Mono.from(beginTransaction)
        .thenMany(fn.apply(this))
        .concatWith(passThrough(() => this.commitTransaction))
        .onErrorResume(appendError(() => this.rollbackTransaction))
  }

  /**
   * Executes the given SQL statement, and transforms each [[Result]]s that are returned from
   * execution.
   *
   * @param sql SQL statement
   * @param fn a function used to transform each [[Result]] into a [[Flux]] of values
   * @tparam T the type of results
   * @return the values resulting from the [[Result]] transformation
   * @throws IllegalArgumentException if `sql` or `fn` is `null`
   */
  def executeQuery[T](sql: String, fn: Result => _ <: Publisher[T]): Flux[T] = {
    require(sql != null, SQL_REQUIRED)
    require(fn != null, FN_REQUIRED)
    Flux.from(this.connection.createStatement(sql).execute).flatMap(fn(_))
  }

  /**
   * Executes the given SQL statement, and transforms each [[Result]]s that are returned from
   * execution.
   *
   * @param sql SQL statement
   * @param fn a function used to transform each [[Result]] into a [[Flux]] of values
   * @tparam T the type of results
   * @return the values resulting from the [[Result]] transformation
   * @throws IllegalArgumentException if `sql` or `fn` is `null`
   */
  def executeQuery[T](sql: String, bindings: Seq[Array[Any]], fn: Result => _ <: Publisher[T]): Flux[T] = {
    require(sql != null, SQL_REQUIRED)
    require(fn != null, FN_REQUIRED)

    val statement = this.connection.createStatement(sql)
    var count = 0
    bindings.foreach(it => {
      it.indices.foreach(i => statement.bind(i, it(i)))
      count += 1;
      if (count < bindings.size) {
        statement.add()
      }
    })

    Flux.from(statement.execute).flatMap(fn(_))
  }

  /**
   * Commits the current transaction.
   *
   * @return a [[Publisher]] that indicates that a transaction has been committed
   */
  private def commitTransaction = this.connection.commitTransaction

  /**
   * Begins a new transaction.
   *
   * @return a [[Publisher]] that indicates that the transaction is open
   */
  private def beginTransaction = this.connection.beginTransaction

  /**
   * Rolls back the current transaction.
   *
   * @return a [[Publisher]] that indicates that a transaction has been rolled back
   */
  private def rollbackTransaction = this.connection.rollbackTransaction

  /**
   * Release any resources held by the [[Handle]].
   *
   * @return a [[Publisher]] that indicates that a termination is complete
   */
  private[client] def close = this.connection.close

}
