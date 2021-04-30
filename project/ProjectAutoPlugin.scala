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

import org.scalafmt.sbt.ScalafmtPlugin.autoImport.scalafmtOnCompile
import sbt.Keys._
import sbt.plugins.JvmPlugin
import sbt.{Def, _}

object ProjectAutoPlugin extends AutoPlugin {

  override val requires = JvmPlugin

  override def trigger = allRequirements

  override def globalSettings: Seq[Def.Setting[_]] = Seq(
    organization := "com.astutebits",
    organizationName := "AstuteBits",
    homepage := Some(url("https://github.com/chmodas/akka-persistence-r2dbc")),
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/chmodas/akka-persistence-r2dbc"),
        "git@github.com:chmodas/akka-persistence-r2dbc"
      )
    ),
    developers += Developer(
      "contributors",
      "Contributors",
      "https://github.com/chmodas/akka-persistence-r2dbc/issues",
      url("https://github.com/chmodas/akka-persistence-r2dbc/graphs/contributors")
    ),
    description := "",
    startYear := Some(2020)
  )

  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    crossVersion := CrossVersion.binary,
    crossScalaVersions := Versions.supportedScala,
    scalaVersion := Versions.scala213,
    scalacOptions ++= Seq(
      "-encoding",
      "utf8",
      "-deprecation",
      "-unchecked",
      if (scalaVersion.value.startsWith("2.13")) "-Wunused" else "-Ywarn-unused",
      "-language:implicitConversions",
      "-language:higherKinds",
      "-language:existentials",
      "-language:postfixOps"
    ),
    scalacOptions += {
      if (scalaVersion.value.startsWith("2.13")) "" else "-Ypartial-unification"
    },
    javacOptions ++= Seq(
      "-encoding",
      "UTF-8",
      "-deprecation",
      "-Xlint:unchecked",
      "-XDignore.symbol.file",
      "-Xlint:deprecation"
    ),
    scalafmtOnCompile := true,
    Test / logBuffered := true,
    Test / testOptions += Tests.Argument("-oDF")
  )
}
