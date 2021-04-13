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

import sbt.Keys._
import sbt._
import xerial.sbt.Sonatype.GitHubHosting
import xerial.sbt.Sonatype.autoImport.{sonatypeProjectHosting, sonatypePublishToBundle}

object Publish extends AutoPlugin {

  override def trigger = allRequirements

  override val projectSettings = Seq(
    publishTo := sonatypePublishToBundle.value,
    sonatypeProjectHosting := Some(GitHubHosting("astutebits", "akka-persistence-r2dbc", "github+issues-only@example.com")),
    credentials += Credentials(
      "GnuPG Key ID",
      "gpg",
      "D121248854D5FFFB2E812D2F1078D3A182FE0520",
      "ignored"
    ),
    credentials += Credentials(
      "Sonatype Nexus Repository Manager",
      "oss.sonatype.org",
      System.getProperty("SONATYPE_USERNAME"),
      System.getProperty("SONATYPE_PASSWORD")
    ),
    startYear := Some(2020),
    licenses += ("Apache-2.0", url("https://apache.org/licenses/LICENSE-2.0")),
    organizationName := "AstuteBits",
    publishMavenStyle := true
  )

}
