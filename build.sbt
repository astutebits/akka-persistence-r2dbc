import sbt.Keys.skip

lazy val root = (project in file("."))
    .settings(
      name := "akka-persistence-r2dbc-root",
      skip in publish := true,
      crossScalaVersions := Nil,
    )
    .aggregate(postgresql, mysql, core)

lazy val `r2dbc-mini-client` = Project(id = "r2dbc-mini-client", base = file("r2dbc-mini-client"))
    .settings(
      name := "r2dbc-mini-client",
      description := "Ultra-minimalistic client for R2DBC drivers.",
      libraryDependencies ++= Dependencies.R2dbcClient
    )

lazy val core = Project(id = "core", base = file("core"))
    .settings(
      name := "akka-persistence-r2dbc",
      description := "Reactive Akka Journal, Snapshot Store, and Persistence Query plugin",
      libraryDependencies ++= Dependencies.Core,
      fork in Test := true
    )
    .dependsOn(`r2dbc-mini-client`)

lazy val postgresql = Project(id = "postgresql", base = file("postgresql"))
    .settings(
      name := "akka-persistence-postgresql",
      libraryDependencies ++= Dependencies.PostgreSQL,
      description := "Reactive Akka Journal, Snapshot Store, and Persistence Query plugin for PostgreSQL",

      fork in Test := true,
      parallelExecution in Test := false
    )
    .dependsOn(core, `r2dbc-mini-client` % "test", tck % "test")

lazy val mysql = Project(id = "mysql", base = file("mysql"))
    .settings(
      name := "akka-persistence-mysql",
      libraryDependencies ++= Dependencies.MySQL,
      description := "Reactive Akka Journal, Snapshot Store, and Persistence Query plugin for MySQL",

      fork in Test := true,
      parallelExecution in Test := false
    )
    .dependsOn(core, `r2dbc-mini-client` % "test", tck % "test")

lazy val tck = Project(id = "tck", base = file("tck"))
    .settings(
      name := "akka-persistence-r2dbc-tck",
      libraryDependencies ++= Dependencies.TCK
    )
    .dependsOn(core)

lazy val `perf-tests` = Project(id = "perf-tests", base = file("perf-tests"))
    .settings(
      name := "akka-persistence-r2dbc-perf-tests",
      skip in publish := true,
      libraryDependencies ++= Dependencies.Perf,

      fork in Test := true
    )
    .dependsOn(postgresql, mysql)

