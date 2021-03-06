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

import org.specs2.{Specification, ScalaCheck}

import scalaz._, Scalaz._

import scalaz.scalacheck.ScalazArbitrary._
import scalaz.scalacheck.ScalazProperties.monad

object Test extends Specification with ScalaCheck { def is = s2"""
  Scalaz arbitraries work $test
  Scalaz laws work        ${monad.laws[Id]}

"""

  def test = prop((s: ValidationNel[String, Int]) =>
    s must_== s
  )
}
