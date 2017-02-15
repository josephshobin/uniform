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
package setting

import sbt._
import Keys._

import au.com.cba.omnia.uniform.core.scala.{Scala11, Scala12}

object ScalaSettings extends Plugin {
  object scala {
    def settings(jvmVersion: String = Scala11.jvmVersion) = settings11(jvmVersion)

    /** Scala 2.11 settings. */
    def settings11(jvmVersion: String = Scala11.jvmVersion) = Seq(
      scalaVersion := Scala11.version,
      crossScalaVersions := Seq(Scala11.version),
      scalacOptions ++= Seq(
        "-deprecation",
        "-unchecked",
        "-Xlint",
        "-Ywarn-dead-code",
        "-Ywarn-value-discard",
        "-Ywarn-unused-import",
        "-feature",
        "-language:_",
        s"-target:jvm-${jvmVersion}"
      ),
      scalacOptions in (Compile, console) ~= (_.filterNot(Set("-Ywarn-unused-import"))),
      scalacOptions in (Test, console) := (scalacOptions in (Compile, console)).value,
      javacOptions ++= Seq(
        "-Xlint:unchecked",
        "-source", jvmVersion,
        "-target", jvmVersion
      )
    )

    /** Scala 2.12 settings. */
    def settings12(jvmVersion: String = Scala12.jvmVersion) = Seq(
      scalaVersion := Scala12.version,
      crossScalaVersions := Seq(Scala12.version),
      scalacOptions ++= Seq(
        "-deprecation",
        "-unchecked",
        "-Xlint",
        "-Ywarn-dead-code",
        "-Ywarn-value-discard",
        "-Ywarn-unused-import",
        "-Ypartial-unification",
        "-feature",
        "-language:_",
        s"-target:jvm-${jvmVersion}"
      ),
      scalacOptions in (Compile, console) ~= (_.filterNot(Set("-Ywarn-unused-import"))),
      scalacOptions in (Test, console) := (scalacOptions in (Compile, console)).value,
      javacOptions ++= Seq(
        "-Xlint:unchecked",
        "-source", jvmVersion,
        "-target", jvmVersion
      )
    )
  }
}
