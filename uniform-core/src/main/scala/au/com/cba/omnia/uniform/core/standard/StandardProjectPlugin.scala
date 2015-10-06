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

import au.com.cba.omnia.uniform.core.setting.ScalaSettings.scala
import au.com.cba.omnia.uniform.core.version.{GitInfo, NotAGitRepository, Dirty, Clean}
import au.com.cba.omnia.uniform.core.version.VersionInfoPlugin.{versionInfoSettings, rootPackage}

object StandardProjectPlugin extends Plugin {
  /** Manually assign link to API pages for the specified package.*/
  def assignApiUrl(classpath: Seq[Attributed[File]], organization: String, name: String, link: String): Option[(File, URL)] = {
    ( for {
      entry  <- classpath
      module <- entry.get(moduleID.key)
      if module.organization == organization
      if module.name.startsWith(name)
      jarFile = entry.data
    } yield jarFile
    ).headOption.map(_ -> url(link))
  }

  object uniform {
    def project(project: String, pkg: String) = List(
      name := project,
      organization := "au.com.cba.omnia",
      rootPackage := pkg
    ) ++ scala.settings ++ versionInfoSettings

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
        site.addMappingsToSiteDir(mappings in (ScalaUnidoc, packageDoc), "latest/api"),
        includeFilter in makeSite := "*.html" | "*.css" | "*.png" | "*.jpg" | "*.gif" | "*.js" | "*.swf" | "*.md" | "*.yml",
        apiURL <<= (baseDirectory).apply(base => Some(url(s"https://commbank.github.io/${base.getName}/latest/api"))),
        scalacOptions in (ScalaUnidoc, unidoc) <++= (version, baseDirectory).map { (v, base) =>
          val hash = GitInfo.commit(base) match {
            case NotAGitRepository => Option.empty
            case Dirty(hash)       => Option(hash)
            case Clean(hash)       => Option(hash)
          }

          val urlSettings = hash.map { h =>
            val docSourceUrl = s"https://github.com/CommBank/${base.getName}/tree/$h/€{FILE_PATH}.scala"
            Seq("-doc-source-url", docSourceUrl)
          }.getOrElse(Seq.empty)

          Seq("-sourcepath", base.getAbsolutePath) ++ urlSettings
        }
      )
  }
}
