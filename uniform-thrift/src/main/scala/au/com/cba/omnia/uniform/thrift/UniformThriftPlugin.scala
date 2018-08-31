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

package au.com.cba.omnia.uniform.thrift

import au.com.cba.omnia.uniform.dependency.UniformDependencyPlugin._
import com.twitter.scrooge.ScroogeSBT._
import sbt._, Keys._

object UniformThriftPlugin extends Plugin {
  def uniformThriftSettings: Seq[Sett] = newSettings ++ Seq[Sett](
    libraryDependencies ++= depend.scrooge(),
    // Work around https://github.com/twitter/scrooge/issues/188
    (scroogeThriftOutputFolder in Compile) := (sourceManaged in Compile).value / "scrooge",
    (scroogeThriftOutputFolder in Test) := (sourceManaged in Test).value / "scrooge",
    // Force scrooge-gen to always be run (it is buggy w.r.t. picking up changes to new thrift files).
    (scroogeIsDirty in Compile) := { (scroogeIsDirty in Compile).value; true }
  )
}
