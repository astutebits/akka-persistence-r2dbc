ThisBuild / scalaVersion := "2.13.1"
ThisBuild / organization := "com.github.chmodas"

lazy val root = (project in file("."))
    .enablePlugins(DockerComposePlugin)
    .settings(
      name := "akka-persistence-r2dbc",
      libraryDependencies ++= Dependencies.Libraries
    )

