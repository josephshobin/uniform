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

import java.io.File
import java.text.SimpleDateFormat
import java.util.{Date, TimeZone}

import sbt._, Keys._

object UniqueVersion extends Plugin {
  def uniqueVersionSettings = Seq(
    version in ThisBuild <<= (version in ThisBuild, baseDirectory) ((v, d) => v + "-" + timestamp(now) + "-" + commish(d))
  )

  def timestamp(instant: Date, format: String = "yyyyMMddHHmmss") = {
    val formatter = new SimpleDateFormat(format)
    formatter.setTimeZone(TimeZone.getTimeZone("UTC"))
    formatter.format(instant)
  }

  def commish(root: File) = gitlog(root, "%h")

  def commit(root: File) = gitlog(root, "%H")

  def gitlog(root: File, format: String) =
    Process("git log --pretty=format:" + format + " -n  1", Some(root)).lines.head

  lazy val now = new Date
}
