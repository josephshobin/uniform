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
      assemblyMergeStrategy in assembly <<= (assemblyMergeStrategy in assembly)(defaultMergeStrategy),
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

  // Set a classifier and corresponding jarName for an assembly, overriding the the sbt-assembly naming.
  def classifierNamingSettings(aClassifier: String, anAssembly: TaskKey[File]) = Seq(
    artifact in (Compile, anAssembly) ~= { _.copy(`classifier` = Some(aClassifier)) },
    assemblyJarName in anAssembly <<= (name, scalaBinaryVersion, version) map {
      (nm, sv, ver) => s"${nm}_${sv}-${ver}-${aClassifier}.jar"
    }
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
