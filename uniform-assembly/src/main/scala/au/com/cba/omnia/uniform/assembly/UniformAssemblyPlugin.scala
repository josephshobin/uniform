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

package au.com.cba.omnia.uniform.assembly

import sbt._, Keys._
import sbtassembly.AssemblyPlugin.autoImport._
import sbtassembly.MergeStrategy

object UniformAssemblyPlugin extends Plugin {
  /** Adds an assembly artifact that includes all dependencies. */
  def uniformAssemblySettings: Seq[Sett] = uniformAssemblySettings(splitPackageDeps = false)

  /** Adds an assembly that includes all dependencies, optionally split into two assemblies (thin and deps). */
  def uniformAssemblySettings(splitPackageDeps: Boolean = false): Seq[Sett] = Seq(
      assemblyMergeStrategy in assembly := defaultMergeStrategy((assemblyMergeStrategy in assembly).value),
      test in assembly := {}
    ) ++ (
      if (splitPackageDeps) splitAssemblySettings
      else Seq(artifact in (Compile, assembly) ~= { _.copy(`classifier` = Some("assembly")) })
    ) ++
    addArtifact(artifact in (Compile, assembly), assembly)

  /** Don't use this directly, instead use `uniformAssemblySettings(splitPackageDeps = true`) */
  def splitAssemblySettings: Seq[Sett] =
    Seq(assemblyOption in assembly ~= { _.copy(includeScala = false, includeDependency = false) }) ++
    classifierNamingSettings("thin", assembly) ++
    classifierNamingSettings("deps", assemblyPackageDependency) ++
    addArtifact(artifact in (Compile, assemblyPackageDependency), assemblyPackageDependency)

  /**
    * This method allows a subproject to re-use a previously-built subproject’s
    * split assembly package dependency jar, if the libraryDependencies and
    * dependencyOverrides are the same.
    * For example, an RPM built with these settings will contain `baseProject-deps.jar`
    * instead of `project-deps.jar`.
    * @param project the subproject that may re-use a `-deps.jar`
    * @param baseProject the subproject already built with the required `-deps.jar`
    */
  def shareSplitAssemblyPackageDependencyJar(project: Project, baseProject: Project): Project = project.settings(
    assemblyPackageDependency := {
      val s = (streams in assemblyPackageDependency).value

      // Helper method to deal with CrossVersion.Full equality issue (affects scala macro paradise)
      val fullSingleton = CrossVersion.full
      def comparableModule(mid: ModuleID): ModuleID =
        mid.copy(crossVersion = mid.crossVersion match {
          case f: CrossVersion.Full => fullSingleton
          case other => other
        })

      // Check if project uses the same libraryDependencies and dependencyOverrides as baseProject
      val baseDeps = (libraryDependencies in baseProject).value.map(comparableModule)
      val baseOverrides = (dependencyOverrides in baseProject).value.map(comparableModule)
      val currentDeps = (libraryDependencies in project).value.map(comparableModule)
      val currentOverrides = (dependencyOverrides in project).value.map(comparableModule)

      if (baseDeps == currentDeps && baseOverrides == currentOverrides) {
        // Use pre-build base deps assembly (saves a lot of time when building many subprojects)
        s.log.info(s"assemblyPackageDependency: Re-using ${baseProject.id} deps assembly for ${project.id}")
        (assemblyOutputPath in assemblyPackageDependency in baseProject).value
      } else {
        // Build our own deps assembly.
        s.log.info(s"assemblyPackageDependency: Building custom deps assembly for ${project.id}")
        // Note: Unfortunately can’t simply use `assemblyPackageDependency.value` here.
        // This is because SBT strictly evaluates .value even if the result is unused!
        // Instead, here is the upstream code from `def assemblyTask` and `assembledMappings` in the plugin
        // https://github.com/sbt/sbt-assembly/blob/master/src/main/scala/sbtassembly/Assembly.scala#L240
        // This prevents SBT from re-running the assembly steps ("Including", "Merging") for every subproject.
        val assembledMappings = sbtassembly.Assembly.assembleMappings(
          (fullClasspath in assembly).value,
          (externalDependencyClasspath in assembly).value,
          (assemblyOption in assemblyPackageDependency).value,
          s.log
        )
        Assembly(
          (assemblyOutputPath in assemblyPackageDependency).value, (assemblyOption in assemblyPackageDependency).value,
          (packageOptions in assemblyPackageDependency).value, assembledMappings,
          s.cacheDirectory, s.log
        )
      }
    }
  )

  // Set a classifier and corresponding jarName for an assembly, overriding the the sbt-assembly naming.
  def classifierNamingSettings(aClassifier: String, anAssembly: TaskKey[File]) = Seq(
    artifact in (Compile, anAssembly) ~= { _.copy(`classifier` = Some(aClassifier)) },
    assemblyJarName in anAssembly := s"${name.value}_${scalaBinaryVersion.value}-${version.value}-${aClassifier}.jar"
  )

  def defaultMergeStrategy(old: String => MergeStrategy) =  (path: String) => path match {
    case "META-INF/LICENSE" => MergeStrategy.rename
    case "META-INF/license" => MergeStrategy.rename
    case "META-INF/NOTICE.txt" => MergeStrategy.rename
    case "META-INF/LICENSE.txt" => MergeStrategy.rename
    case "META-INF/MANIFEST.MF" => MergeStrategy.discard
    case "application.conf" => MergeStrategy.concat
    case "reference.conf"   => MergeStrategy.concat
    case PathList("META-INF", xs) if xs.toLowerCase.endsWith(".dsa") => MergeStrategy.discard
    case PathList("META-INF", xs) if xs.toLowerCase.endsWith(".rsa") => MergeStrategy.discard
    case PathList("META-INF", xs) if xs.toLowerCase.endsWith(".sf") => MergeStrategy.discard
    case _ => MergeStrategy.first
  }
}
