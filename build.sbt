import sbt.Keys.skip

ThisBuild / scalaVersion := "2.13.1"
ThisBuild / organization := "com.astutebits"
ThisBuild / javacOptions ++= Seq(
  "-encoding", "UTF-8",
  "-deprecation",
  "-Xlint:unchecked",
  "-XDignore.symbol.file",
  "-Xlint:deprecation"
)
ThisBuild / scalacOptions ++= Seq(
  "-encoding", "utf8",
  "-deprecation",
  "-unchecked",
  "-language:implicitConversions",
  "-language:higherKinds",
  "-language:existentials",
  "-language:postfixOps"
)

lazy val root = (project in file("."))
    .enablePlugins(DockerComposePlugin)
    .settings(
      name := "akka-persistence-r2dbc",
      skip in publish := true,

      // We are only using DockerCompose for testing.
      dockerImageCreationTask := ""
    )
    .aggregate(postgresql, mysql, core)

lazy val core = Project(id = "core", base = file("core"))
    .settings(
      name := "akka-persistence-r2dbc-core",
      skip in publish := true,
      libraryDependencies ++= Dependencies.Core
    )
    .settings(fork in Test := true)

lazy val postgresql = Project(id = "postgresql", base = file("postgresql"))
    .settings(
      name := "akka-persistence-postgresql",
      libraryDependencies ++= Dependencies.PostgreSQL
    )
    .settings(
      fork in Test := true,
      parallelExecution in Test := false
    )
    .dependsOn(core)

lazy val mysql = Project(id = "mysql", base = file("mysql"))
    .settings(
      name := "akka-persistence-mysql",
      libraryDependencies ++= Dependencies.MySQL
    )
    .settings(
      fork in Test := true,
      parallelExecution in Test := false
    )
    .dependsOn(core)

lazy val `perf-tests` = Project(id = "perf-tests", base = file("perf-tests"))
    .settings(
      name := "akka-persistence-r2dbc-perf-tests",
      skip in publish := true,
      libraryDependencies ++= Dependencies.Perf
    )
    .settings(fork in Test := true)
    .dependsOn(postgresql, mysql)

