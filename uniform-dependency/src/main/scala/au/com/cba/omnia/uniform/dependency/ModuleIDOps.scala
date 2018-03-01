package au.com.cba.omnia.uniform.dependency

import sbt.ModuleID

import au.com.cba.omnia.uniform.core.scala

class ModuleIDOps(module: ModuleID) {
  /**
    * Exclude packages for all scala binary versions.
    *
    * Until we get a fix for https://github.com/sbt/sbt/issues/1518 it is really hard to do this
    * properly so this takes a brute force approach and just excludes packages for each binary
    * version. */
  def scalaExcludeAll(group: String, artifact: String): ModuleID =
    scala.allBinaryVersions.foldLeft(module)((m, v) => m.exclude(group, s"${artifact}_${v}"))
}
