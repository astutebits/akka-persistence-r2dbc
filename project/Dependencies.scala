import sbt._

object Versions {
  lazy val akka = "2.6.8"
  lazy val reactor = "3.3.6.RELEASE"
  lazy val r2dbcSpi = "0.8.2.RELEASE"
  lazy val r2dbcPostgreSql = "0.8.5.BUILD-SNAPSHOT"
  lazy val r2dbcMySql = "0.8.2.RELEASE"
  lazy val scalaTest = "3.1.1"
  lazy val mockito = "1.14.2"
}

object Dependencies {

  private val Base = Seq(
    "ch.qos.logback" % "logback-classic" % "1.2.3" % Test,
    "org.scalatest" %% "scalatest" % Versions.scalaTest % Test
  )

  private val Libraries = Base ++ Seq(
    "com.typesafe.akka" %% "akka-persistence-tck" % Versions.akka % Test
  )

  val Core = Base ++ Seq(
    "io.projectreactor" % "reactor-core" % Versions.reactor,
    "com.typesafe.akka" %% "akka-persistence-query" % Versions.akka,
    "io.r2dbc" % "r2dbc-spi" % Versions.r2dbcSpi,
    "io.r2dbc" % "r2dbc-pool" % "0.8.3.RELEASE",

    "org.mockito" %% "mockito-scala-scalatest" % Versions.mockito % Test,
    "io.r2dbc" % "r2dbc-spi-test" % Versions.r2dbcSpi % Test,
    "com.typesafe.akka" %% "akka-stream-testkit" % Versions.akka % Test
  )

  val PostgreSQL: Seq[ModuleID] = Libraries ++ Seq(
    "io.r2dbc" % "r2dbc-postgresql" % Versions.r2dbcPostgreSql
  )

  val MySQL: Seq[ModuleID] = Libraries ++ Seq(
    "dev.miku" % "r2dbc-mysql" % Versions.r2dbcMySql
  )

  val TCK: Seq[ModuleID] = Seq(
    "com.typesafe.akka" %% "akka-persistence-query" % Versions.akka,
    "org.scalatest" %% "scalatest" % Versions.scalaTest,
    "com.typesafe.akka" %% "akka-stream-testkit" % Versions.akka
  )

  val Perf: Seq[ModuleID] = Libraries

}
