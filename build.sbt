ThisBuild / scalaVersion := "2.13.1"
ThisBuild / organization := "com.astutebits"
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

      // We are only using DockerCompose for testing.
      dockerImageCreationTask := ""
    )
    .aggregate(postgresql)

lazy val postgresql = Project(id = "postgresql", base = file("postgresql"))
    .settings(
      name := "akka-persistence-postgresql",
      libraryDependencies ++= Dependencies.Libraries,
    )