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

import sbt._

object Versions {
  lazy val scala212 = "2.12.15"
  lazy val scala213 = "2.13.8"
  lazy val supportedScala = Seq(scala213, scala212)

  lazy val akka = "2.6.18"
  lazy val reactor = "3.4.28"
  lazy val r2dbcSpi = "0.8.6.RELEASE"
  lazy val r2dbcPool = "0.8.8.RELEASE"
  lazy val r2dbcPostgreSql = "0.8.11.RELEASE"
  lazy val r2dbcMySql = "0.8.2.RELEASE"
  lazy val scalaTest = "3.2.11"
  lazy val mockito = "1.16.55"
}

object Dependencies {

  private val Base = Seq(
    "ch.qos.logback" % "logback-classic" % "1.2.7" % Test,
    "org.scalatest" %% "scalatest" % Versions.scalaTest % Test)

  private val Libraries = Base ++ Seq("com.typesafe.akka" %% "akka-persistence-tck" % Versions.akka % Test)

  val R2dbcClient: Seq[ModuleID] = Base ++ Seq(
    "io.r2dbc" % "r2dbc-spi" % Versions.r2dbcSpi,
    "io.projectreactor" % "reactor-core" % Versions.reactor,
    "io.r2dbc" % "r2dbc-spi-test" % Versions.r2dbcSpi % Test)

  val Core: Seq[ModuleID] = Base ++ Seq(
    "com.typesafe.akka" %% "akka-persistence-query" % Versions.akka,
    "io.r2dbc" % "r2dbc-pool" % Versions.r2dbcPool,
    "org.mockito" %% "mockito-scala-scalatest" % Versions.mockito % Test,
    "com.typesafe.akka" %% "akka-stream-testkit" % Versions.akka % Test)

  val PostgreSQL: Seq[ModuleID] = Libraries ++ Seq("io.r2dbc" % "r2dbc-postgresql" % Versions.r2dbcPostgreSql)

  val MySQL: Seq[ModuleID] = Libraries ++ Seq("dev.miku" % "r2dbc-mysql" % Versions.r2dbcMySql)

  val TCK: Seq[ModuleID] = Seq(
    "com.typesafe.akka" %% "akka-persistence-query" % Versions.akka,
    "org.scalatest" %% "scalatest" % Versions.scalaTest,
    "com.typesafe.akka" %% "akka-stream-testkit" % Versions.akka)

  val Perf: Seq[ModuleID] = Libraries

}
