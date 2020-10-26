import sbt.Keys.skip

lazy val root = (project in file("."))
    .enablePlugins(DockerComposePlugin)
    .settings(
      name := "akka-persistence-r2dbc-root",
      skip in publish := true,
      crossScalaVersions := Nil,

      // We are only using DockerCompose for testing.
      dockerImageCreationTask := ""
    )
    .aggregate(postgresql, mysql, core)

lazy val core = Project(id = "core", base = file("core"))
    .settings(
      name := "akka-persistence-r2dbc",
      description := "Reactive Akka Journal, Snapshot Store, and Persistence Query plugin",
      libraryDependencies ++= Dependencies.Core,
      fork in Test := true
    )

lazy val postgresql = Project(id = "postgresql", base = file("postgresql"))
    .settings(
      name := "akka-persistence-postgresql",
      libraryDependencies ++= Dependencies.PostgreSQL,
      description := "Reactive Akka Journal, Snapshot Store, and Persistence Query plugin for PostgreSQL",

      fork in Test := true,
      parallelExecution in Test := false
    )
    .dependsOn(core, tck % "test")

lazy val mysql = Project(id = "mysql", base = file("mysql"))
    .settings(
      name := "akka-persistence-mysql",
      libraryDependencies ++= Dependencies.MySQL,
      description := "Reactive Akka Journal, Snapshot Store, and Persistence Query plugin for MySQL",

      fork in Test := true,
      parallelExecution in Test := false
    )
    .dependsOn(core, tck % "test")

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

