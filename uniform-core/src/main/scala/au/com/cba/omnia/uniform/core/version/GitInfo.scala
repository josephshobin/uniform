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
package version

import _root_.scala.Console.{RED, BOLD, RESET}

import sbt._, Keys._

sealed abstract class GitStatus {
  def show = this match {
    case NotAGitRepository => "UNTRACKED"
    case Dirty(hash)       => hash + "-SNAPSHOT"
    case Clean(hash)       => hash
  }

  def hashOption: Option[String] = this match {
    case NotAGitRepository => None
    case Clean(hash)       => Some(hash)
    case Dirty(hash)       => Some(hash)
  }
}

case object NotAGitRepository extends GitStatus

case class Clean(hash: String) extends GitStatus

case class Dirty(hash: String) extends GitStatus

object GitStatus {
  def apply(root: File, format: String): GitStatus =
    if (!isGitRepository(root)) NotAGitRepository
    else {
      val lastCommit = Process(s"git log --pretty=format:$format -n 1", cwd = Some(root)).lines.head
      if (isGitRepositoryClean(root))
        Clean(lastCommit)
      else
        Dirty(lastCommit)
    }

  private def isGitRepository(root: File): Boolean =
    Process("git status", cwd = Some(root)) ! OnlyLogStdErr == 0

  private def isGitRepositoryClean(root: File): Boolean =
    Process("git status --porcelain", cwd = Some(root)).lines.isEmpty
}

/** Logs any STDERR output in bold red and discards STDOUT output */
object OnlyLogStdErr extends ProcessLogger {
  override def buffer[T](f: => T) = f
  override def error(s: => String) = println(s"$RED$BOLD$s$RESET")
  override def info(s: => String) = {}
}

object GitInfo {
  def commish(root: File): GitStatus = GitStatus(root, "%h")

  def commit(root: File): GitStatus = GitStatus(root, "%H")

}
