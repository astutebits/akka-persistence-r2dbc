import sbt._

object Versions {
  lazy val akka = "2.6.4"
  lazy val r2dbc = "0.8.2.RELEASE"
  lazy val r2dbcSpi = "0.8.1.RELEASE"
  lazy val scalaTest = "3.1.1"
  lazy val mockito = "1.14.2"
}

object Dependencies {

  val Core = Seq(
    "io.projectreactor" % "reactor-core" % "3.3.4.RELEASE",
    "com.typesafe.akka" %% "akka-stream-typed" % Versions.akka,
    "com.typesafe.akka" %% "akka-persistence-query" % Versions.akka,
    "io.r2dbc" % "r2dbc-spi" % Versions.r2dbcSpi,

    "org.scalatest" %% "scalatest" % Versions.scalaTest % Test,
    "org.mockito" %% "mockito-scala-scalatest" % Versions.mockito % Test,
    "io.r2dbc" % "r2dbc-spi-test" % Versions.r2dbcSpi % Test,
    "com.typesafe.akka" %% "akka-actor-testkit-typed" % Versions.akka % Test,
    "com.typesafe.akka" %% "akka-stream-testkit" % Versions.akka % Test,
    "ch.qos.logback" % "logback-classic" % "1.2.3" % Test
  )

  private val Libraries = Seq(
    "com.typesafe.akka" %% "akka-persistence-query" % Versions.akka,

    "ch.qos.logback" % "logback-classic" % "1.2.3" % Test,
    "org.scalatest" %% "scalatest" % Versions.scalaTest % Test,
    "com.typesafe.akka" %% "akka-stream-testkit" % Versions.akka % Test,
    "com.typesafe.akka" %% "akka-persistence-tck" % Versions.akka % Test
  )

  val PostgreSQL: Seq[ModuleID] = Libraries ++ Seq(
    "io.r2dbc" % "r2dbc-postgresql" % Versions.r2dbc
  )

  val MySQL: Seq[ModuleID] = Libraries ++ Seq(
    "dev.miku" % "r2dbc-mysql" % "0.8.1.RELEASE"
  )

  val TCK: Seq[ModuleID] = Seq(
    "com.typesafe.akka" %% "akka-persistence-query" % Versions.akka,
    "org.scalatest" %% "scalatest" % Versions.scalaTest ,
    "com.typesafe.akka" %% "akka-stream-testkit" % Versions.akka
  )

  val Perf: Seq[ModuleID] = Libraries ++ Seq(
    "io.r2dbc" % "r2dbc-postgresql" % Versions.r2dbc,
    "dev.miku" % "r2dbc-mysql" % "0.8.1.RELEASE"
  )
}
