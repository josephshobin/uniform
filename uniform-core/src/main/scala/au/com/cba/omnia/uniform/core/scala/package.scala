package au.com.cba.omnia.uniform.core

package object scala {
  val Scala = Scala11

  /**
    * Includes all the Scala binary versions we might deal with.
    *
    * In particular, this is used by ModuleIdOps to brute force the exclusion of scala dependencies.
    */
  val allBinaryVersions = List("2.10", Scala11.binaryVersion, Scala12.binaryVersion)
}
