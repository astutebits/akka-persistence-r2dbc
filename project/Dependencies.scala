import sbt._

object Versions {
  lazy val akka = "2.6.4"
  lazy val r2dbc = "0.8.2.RELEASE"
  lazy val scalaTest = "3.1.1"
}

object Dependencies {

  val Libraries = Seq(
    "com.typesafe.akka" %% "akka-actor-typed" % Versions.akka,
    "com.typesafe.akka" %% "akka-stream-typed" % Versions.akka,
    "com.typesafe.akka" %% "akka-persistence-typed" % Versions.akka,
    "com.typesafe.akka" %% "akka-persistence-query" % Versions.akka,

    "io.r2dbc" % "r2dbc-postgresql" % Versions.r2dbc,

    "ch.qos.logback" % "logback-classic" % "1.2.3" % Test,
    "org.scalatest" %% "scalatest" % Versions.scalaTest % Test,
    "com.typesafe.akka" %% "akka-actor-testkit-typed" % Versions.akka % Test,
    "com.typesafe.akka" %% "akka-stream-testkit" % Versions.akka % Test,
    "com.typesafe.akka" %% "akka-persistence-tck" % Versions.akka % Test
  )

}
