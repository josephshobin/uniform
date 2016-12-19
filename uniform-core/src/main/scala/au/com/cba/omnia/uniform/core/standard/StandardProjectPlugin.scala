//   Copyright 2014 Commonwealth Bank of Australia
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.

package au.com.cba.omnia.uniform.core
package standard

import sbt._, Keys._

import sbtunidoc.Plugin._, UnidocKeys._

import com.typesafe.sbt.SbtSite._, SiteKeys._

import au.com.cba.omnia.uniform.core.scala.Scala
import au.com.cba.omnia.uniform.core.setting.ScalaSettings.scala
import au.com.cba.omnia.uniform.core.version.GitInfo
import au.com.cba.omnia.uniform.core.version.VersionInfoPlugin.{versionInfoSettings, rootPackage}

object StandardProjectPlugin extends Plugin {

  /** Manually assign link to API pages for the specified package. */
  def assignApiUrl(classpath: Seq[Attributed[File]], organization: String, name: String, link: String): Option[(File, URL)] = {

    val files = for {
      entry <- classpath
      module <- entry.get(moduleID.key)
      if module.organization == organization
      if module.name.startsWith(name)
      jarFile = entry.data
    } yield jarFile

    files.headOption map (_ -> url(link))
  }

  object uniform {
    lazy val docRootUrl = SettingKey[String]("doc-root-url", "Github Pages root URL (e.g. https://commbank.github.io)")
    lazy val docSourceUrl = SettingKey[String]("doc-source-url", "Github or Github Enterprise root URL (e.g. https://github.com/CommBank")

    def project(project: String, pkg: String, org: String = "omnia", jvmVersion: String = Scala.jvmVersion) = List(
      name := project,
      organization := s"au.com.cba.$org",
      rootPackage := pkg
    ) ++ scala.settings(jvmVersion = jvmVersion) ++ versionInfoSettings

    /** Settings for each sbt project and subproject to create api mappings and expose api url.*/
    def docSettings(link: String): Seq[sbt.Setting[_]] = Seq(
      autoAPIMappings := true,
      apiMappings in (ScalaUnidoc, unidoc) <++= (fullClasspath in Compile).map(cp => Seq(
        assignApiUrl(cp, "cascading", "cascading-core", "http://docs.cascading.org/cascading/2.5/javadoc"),
        assignApiUrl(cp, "cascading", "cascading-hadoop", "http://docs.cascading.org/cascading/2.5/javadoc"),
        assignApiUrl(cp, "cascading", "cascading-local", "http://docs.cascading.org/cascading/2.5/javadoc"),
        assignApiUrl(cp, "com.twitter", "scalding-core", "http://twitter.github.io/scalding/")
      ).flatten.toMap),
      apiURL := Some(url(link))
    )

    /** Settings to create content for github pages. Should only be use by the root project.*/
    def ghsettings: Seq[sbt.Setting[_]] =
      unidocSettings ++ site.settings ++ Seq(
        docRootUrl    := "https://commbank.github.io",
        docSourceUrl  := "https://github.com/CommBank",
        site.addMappingsToSiteDir(mappings in (ScalaUnidoc, packageDoc), "latest/api"),
        includeFilter in makeSite := "*.html" | "*.css" | "*.png" | "*.jpg" | "*.gif" | "*.js" | "*.swf" | "*.md" | "*.yml",
        apiURL <<= (baseDirectory, docRootUrl)((base, docRoot) => Some(url(s"$docRoot/${base.getName}/latest/api"))),
        scalacOptions in (ScalaUnidoc, unidoc) <++= (version, baseDirectory, docSourceUrl).map { (v, base, sourceRoot) =>
          val urlSettings =
            GitInfo.commit(base).hashOption.toSeq flatMap { h =>
              Seq("-doc-source-url", s"$sourceRoot/${ base.getName }/tree/$h/€{FILE_PATH}.scala")
            }

          Seq("-sourcepath", base.getAbsolutePath) ++ urlSettings
        }
      )

    /** Adds settings for crossbuilding against Scala 2.10. */
    def crossBuild = Seq(
      crossScalaVersions := Seq(scalaVersion.value, "2.10.6"),
      scalacOptions      := scalacOptions.value.filter(o =>
        !(scalaBinaryVersion.value == "2.10" && o == "-Ywarn-unused-import")
      )
    )
  }
}
