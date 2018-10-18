//   Copyright 2014-2018 Commonwealth Bank of Australia
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

scala.settings()

uniformDependencySettings

strictDependencySettings

version := "0.1"

libraryDependencies ++=
  depend.hadoopClasspath ++
  depend.hive()          ++
  depend.parquet()       ++
  depend.parquetTools()  ++
  depend.scalding()      ++
  depend.scrooge()       ++
  depend.sqoop()         ++
  // fp
  depend.scalaz()        ++
  depend.scalazStream()  ++
  depend.shapeless()     ++
  // testing
  depend.scalatest()     ++
  depend.mail()          ++
  depend.testing()       ++
  // time
  depend.jodaConvert()   ++
  depend.time()          ++
  // logging
  depend.logging()       ++
  depend.scallop()       ++
  depend.tsLogging()     ++
  // databases
  depend.hsqldb()        ++
  depend.postgresql()    ++
  depend.scalikejdbc()   ++
  // others
  depend.argonaut()      ++
  depend.playJson()      ++
  depend.semver()        ++
  depend.tsConfig()
