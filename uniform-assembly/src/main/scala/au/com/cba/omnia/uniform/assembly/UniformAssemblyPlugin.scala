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
  def uniformAssemblySettings: Seq[Sett] = Seq(
      assemblyMergeStrategy in assembly <<= (assemblyMergeStrategy in assembly)(defaultMergeStrategy),
      test in assembly := {},
      artifact in (Compile, assembly) ~= { art =>
        art.copy(`classifier` = Some("assembly"))
      }
    ) ++ addArtifact(artifact in (Compile, assembly), assembly)

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
