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

import java.lang.management.{ ManagementFactory, MemoryType }

import _root_.scala.collection.JavaConverters._

import sbt._, Keys._

import sbtunidoc.Plugin._, UnidocKeys._

import com.typesafe.sbt.SbtSite._, SiteKeys._

import au.com.cba.omnia.uniform.core.scala.{Scala11, Scala12}
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

    def project(project: String, pkg: String, org: String = "omnia", jvmVersion: String = Scala11.jvmVersion) =
      project11(project, pkg, org, jvmVersion)

    def project11(project: String, pkg: String, org: String = "omnia", jvmVersion: String = Scala11.jvmVersion) = List(
      name := project,
      organization := s"au.com.cba.$org",
      rootPackage := pkg
    ) ++ scala.settings11(jvmVersion = jvmVersion) ++ versionInfoSettings


    def project12(project: String, pkg: String, org: String = "omnia", jvmVersion: String = Scala12.jvmVersion) = List(
      name := project,
      organization := s"au.com.cba.$org",
      rootPackage := pkg
    ) ++ scala.settings12(jvmVersion = jvmVersion) ++ versionInfoSettings


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

    /** Adds settings to pass on JVM max memory config to forked JVMs.
     *
     * @param multipleMB Round the max memory settings up to the nearest multiple of this figure, in megabytes. Defaults to 1024.
     */
    def forkWithMemorySettings(multipleMB: Long = 1024): Seq[sbt.Setting[_]] = {
      if (multipleMB <= 0) throw new IllegalArgumentException(s"max memory multiple must be >= 1 megabytes")

      def roundMB(bytes: Long) = {
        val multiple = multipleMB * 1024 * 1024;
        val rounded = if (bytes % multiple == 0) bytes else multiple * (1 + bytes / multiple)
        rounded / (1024 * 1024)
      }

      val pools = ManagementFactory.getMemoryPoolMXBeans.asScala

      // in all recent JVM versions I am aware of, the max heap setting is divided between the HEAP generations
      // with two copies of the Survivor generation (one copy populated, one empty), totalling slightly less than max heap

      val rawHeapMax = pools
        .filter(_.getType == MemoryType.HEAP)
        .map(pool => pool.getUsage.getMax * (if (pool.getName.contains("Survivor")) 2 else 1))
        .sum
      val heapMaxMB = roundMB(rawHeapMax)

      // if we are running JVM 8 or later, we shouldn't find a Perm Gen pool
      val heapSettings = Seq(javaOptions += s"-Xmx${heapMaxMB}M")

      val permGenSettings =
        pools.find(_.getName.contains("Perm Gen")).map(pool => {
          val permGenMaxMB = roundMB(pool.getUsage.getMax)
          javaOptions += s"-XX:MaxPermSize=${permGenMaxMB}M"
        })

      heapSettings ++ permGenSettings
    }
  }
}
