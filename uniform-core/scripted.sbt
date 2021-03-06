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

ScriptedPlugin.scriptedSettings


scriptedLaunchOpts := {
  scriptedLaunchOpts.value ++
  Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
}

scriptedBufferLog := false

scriptedRun := scriptedRun.dependsOn(publishLocal in core).value

credentials ++= {
  val out = credentials.value.map {
    case c: FileCredentials   => s"""Credentials(new java.io.File("${c.path.getAbsolutePath}"))"""
    case c: DirectCredentials => s"Credentials(${c.realm}, ${c.host}, ${c.userName}, ${c.passwd})"
  }.mkString(" credentials ++= Seq(", ",", ")")
  sbtTestDirectory.value.listFiles.flatMap(_.listFiles).map(f => IO.writeLines(f / "credentials.sbt", Seq(out)))
  List()
}
