import sbt._
import sbt.Keys._

object Publish extends AutoPlugin {
  import bintray.BintrayPlugin
  import bintray.BintrayKeys._

  override def trigger = allRequirements
  override def requires = BintrayPlugin

  override val projectSettings = Seq(
    licenses += ("Apache-2.0", url("https://apache.org/licenses/LICENSE-2.0")),
    bintrayOrganization := Some("astutebits"),
    bintrayRepository := (if (isSnapshot.value) "snapshots" else "maven"),
    publishMavenStyle := true,
    bintrayPackageLabels := Seq("akka", "persistence", "r2dbc")
  )

}
