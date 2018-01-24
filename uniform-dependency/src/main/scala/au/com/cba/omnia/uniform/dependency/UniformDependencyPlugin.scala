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

package au.com.cba.omnia.uniform.dependency

import sbt._, Keys._

import au.com.cba.omnia.uniform.core.scala
import au.com.cba.omnia.uniform.core.scala.Scala

object UniformDependencyPlugin extends Plugin {
  def uniformDependencySettings: Seq[Sett] = uniformPublicDependencySettings ++ uniformPrivateDependencySettings

  def uniformPublicDependencySettings: Seq[Sett] = Seq[Sett](
    resolvers ++= Seq(
      "snapshots" at "http://oss.sonatype.org/content/repositories/snapshots"
    , "releases" at "http://oss.sonatype.org/content/repositories/releases"
    , "Concurrent Maven Repo" at "http://conjars.org/repo"
    , "Clojars Repository" at "http://clojars.org/repo"
    , "Twitter Maven" at "https://maven.twttr.com"
    , "Hadoop Releases" at "https://repository.cloudera.com/content/repositories/releases/"
    , "cloudera" at "https://repository.cloudera.com/artifactory/cloudera-repos/"
    , "commbank-releases" at "http://commbank.artifactoryonline.com/commbank/ext-releases-local"
    , "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"
    )
  )

  def uniformPrivateDependencySettings: Seq[Sett] = Seq[Sett](
    resolvers ++= Seq(
      "commbank-releases-private"     at "https://commbank.artifactoryonline.com/commbank/libs-releases-local"
    , "commbank-releases-private-ivy" at "https://commbank.artifactoryonline.com/commbank/libs-releases-local-ivy"
    )
  )

  /**
    * Enable strict conflict management
    *
    * Add some dependency overrides where necesary to help avoid conflicts:
    *   1) Override scala versions, as we always know what versions we should be using
    *   2) Override conflicting versions of jars which are:
    *      a) not on the hadoop classpath and hence not provided via depend.hadoopClasspath
    *      b) imported from different depend.foo methods, so we aren't sure which of the
    *         depend.foo methods the user will use, hence we can't pick a "canonical"
    *         dependency which includes tha jar, while excluding it from the others
    *   3) Override conflicting versions where I haven't found a better solution
    */
  val strictDependencySettings: Seq[Sett] = Seq[Sett](
    conflictManager := ConflictManager.strict,

    dependencyOverrides <+= scalaVersion(sv => "org.scala-lang" % "scala-library"  % sv),
    dependencyOverrides <+= scalaVersion(sv => "org.scala-lang" % "scala-compiler" % sv),
    dependencyOverrides <+= scalaVersion(sv => "org.scala-lang" % "scala-reflect"  % sv),

    // depend.hive vs. depend.scrooge vs. parquet-cascading
    dependencyOverrides += "org.apache.thrift"   % "libthrift" % depend.versions.libthrift,

    // depend.testing (specs2) vs. depend.scalding (scalding)
    dependencyOverrides += "org.objenesis"       % "objenesis" % depend.versions.objenesis,

    // cascading-hive (hive-exec) and sqoop vs. avro-mapred
    dependencyOverrides += "org.apache.velocity" % "velocity"  % "1.7",

    // depend.hive (hive-exec) vs. depend.scalding (cascading-core)
    dependencyOverrides += "org.codehaus.janino" % "janino"  % "2.7.6",

    // depend.hive (hive-exec) vs. depend.time (joda-time)
    dependencyOverrides += "joda-time" % "joda-time"  % depend.versions.jodaTime,

    // depend.scalding (scalding-core) vs. CDH 5.13.0 (hadoop libraries)
    dependencyOverrides += "org.slf4j" % "slf4j-api"  % depend.versions.slf4j,

    // depend.hive (hive-exec) vs. sqoop
    dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-databind"    % depend.versions.jacksonV2,
    dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-annotations" % depend.versions.jacksonV2,

    // override the jackson-mapper jar versions, to workaround a dependency on the
    // non-hadoop version of these jars being added to the internal ivy configurations,
    // which I haven't figured out how to prevent
    dependencyOverrides += "org.codehaus.jackson" % "jackson-mapper-asl" % depend.versions.jacksonV1,
    dependencyOverrides += "org.codehaus.jackson" % "jackson-core-asl"   % depend.versions.jacksonV1
  )

  /** Exclude provided hadoop jars from a ModuleID */
  def noHadoop(module: ModuleID) = module.copy(
    exclusions = module.exclusions ++ hadoopCP.exclusions
  )

  object hadoopCP {
    // These versions should track the current cluster environment (but do not fear; experience
    // suggests that everything will still work if they are a few point versions behind).
    //
    // Updating them is a manual process. For example, inspect the names of the files on the
    // hadoop classpath by running this command on the integration server:
    //     hadoop classpath | xargs -d ':' -L 1 -i bash -c "echo {}" | tr ' ' '\n'
    // If the filename does not contain version, check inside the jar:
    //     unzip -p /usr/lib/hadoop//parquet-avro.jar \*/MANIFEST.MF \*/pom.properties
    val modules = List[ModuleID](
      "org.apache.hadoop"            % "hadoop-core"               % depend.versions.hadoop,
      "org.apache.hadoop"            % "hadoop-tools"              % depend.versions.hadoop,
      "org.apache.hadoop"            % "hadoop-annotations"        % depend.versions.hadoopNoMr1,
      "org.apache.hadoop"            % "hadoop-auth"               % depend.versions.hadoopNoMr1,
      "org.apache.hadoop"            % "hadoop-common"             % depend.versions.hadoopNoMr1,
      "org.apache.hadoop"            % "hadoop-hdfs"               % depend.versions.hadoopNoMr1,
      "org.apache.hadoop"            % "hadoop-hdfs-nfs"           % depend.versions.hadoopNoMr1,
      "org.apache.hadoop"            % "hadoop-nfs"                % depend.versions.hadoopNoMr1,
      "org.apache.hadoop"            % "hadoop-yarn-api"           % depend.versions.hadoopNoMr1,
      "org.apache.hadoop"            % "hadoop-yarn-client"        % depend.versions.hadoopNoMr1,
      "org.apache.hadoop"            % "hadoop-yarn-common"        % depend.versions.hadoopNoMr1,
      "org.apache.hadoop"            % "hadoop-yarn-server-common" % depend.versions.hadoopNoMr1,
      "com.twitter"                  % "parquet-avro"              % depend.versions.parquet,
      "com.twitter"                  % "parquet-column"            % depend.versions.parquet,
      "com.twitter"                  % "parquet-common"            % depend.versions.parquet,
      "com.twitter"                  % "parquet-encoding"          % depend.versions.parquet,
      "com.twitter"                  % "parquet-generator"         % depend.versions.parquet,
      "com.twitter"                  % "parquet-hadoop"            % depend.versions.parquet,
      "com.twitter"                  % "parquet-jackson"           % depend.versions.parquet,
      "com.twitter"                  % "parquet-format"            % depend.versions.parquetFormat,
      "org.slf4j"                    % "slf4j-api"                 % depend.versions.slf4j,
      "org.slf4j"                    % "slf4j-log4j12"             % depend.versions.slf4j,
      "log4j"                        % "log4j"                     % depend.versions.log4j,
      "commons-beanutils"            % "commons-beanutils"         % "1.9.2",
      "commons-beanutils"            % "commons-beanutils-core"    % "1.8.0",
      "commons-cli"                  % "commons-cli"               % "1.2",
      "commons-codec"                % "commons-codec"             % "1.4",
      "commons-collections"          % "commons-collections"       % "3.2.1",
      "org.apache.commons"           % "commons-compress"          % "1.4.1",
      "commons-configuration"        % "commons-configuration"     % "1.6",
      "commons-daemon"               % "commons-daemon"            % "1.0.13",
      "commons-digester"             % "commons-digester"          % "1.8",
      "commons-el"                   % "commons-el"                % "1.0",
      "commons-httpclient"           % "commons-httpclient"        % "3.1",
      "commons-io"                   % "commons-io"                % "2.4",
      "commons-lang"                 % "commons-lang"              % "2.6",
      "commons-logging"              % "commons-logging"           % "1.1.3",
      "commons-net"                  % "commons-net"               % "3.1",
      "org.apache.commons"           % "commons-math3"             % "3.1.1",
      "org.apache.httpcomponents"    % "httpclient"                % "4.2.5",
      "org.apache.httpcomponents"    % "httpcore"                  % "4.2.5",
      "org.apache.avro"              % "avro"                      % depend.versions.avro,
      "org.apache.zookeeper"         % "zookeeper"                 % depend.versions.zookeeper,
      "com.google.code.findbugs"     % "jsr305"                    % "3.0.0",
      "com.google.guava"             % "guava"                     % depend.versions.guava,
      "com.google.protobuf"          % "protobuf-java"             % "2.5.0",
      "com.google.inject"            % "guice"                     % "3.0",
      "com.google.inject.extensions" % "guice-servlet"             % "3.0",
      "org.codehaus.jackson"         % "jackson-mapper-asl"        % depend.versions.jacksonV1,
      "org.codehaus.jackson"         % "jackson-core-asl"          % depend.versions.jacksonV1,
      "org.codehaus.jackson"         % "jackson-jaxrs"             % depend.versions.jacksonV1,
      "org.codehaus.jackson"         % "jackson-xc"                % depend.versions.jacksonV1,
      "org.codehaus.jettison"        % "jettison"                  % "1.1",
      "org.xerial.snappy"            % "snappy-java"               % "1.0.4.1",
      "junit"                        % "junit"                     % "4.11",
      "jline"                        % "jline"                     % "2.11",
      "org.mortbay.jetty"            % "jetty"                     % depend.versions.jetty,
      "org.mortbay.jetty"            % "jetty-util"                % depend.versions.jetty,
      "hsqldb"                       % "hsqldb"                    % "1.8.0.10",
      "ant-contrib"                  % "ant-contrib"               % "1.0b3",
      "aopalliance"                  % "aopalliance"               % "1.0",
      "javax.inject"                 % "javax.inject"              % "1",
      "javax.xml.bind"               % "jaxb-api"                  % "2.2.2",
      "com.sun.xml.bind"             % "jaxb-impl"                 % "2.2.3-1",
      "javax.servlet"                % "servlet-api"               % "2.5",
      "javax.xml.stream"             % "stax-api"                  % "1.0-2",
      "javax.activation"             % "activation"                % "1.1",
      "com.sun.jersey"               % "jersey-client"             % "1.9",
      "com.sun.jersey"               % "jersey-core"               % "1.9",
      "com.sun.jersey"               % "jersey-server"             % "1.9",
      "com.sun.jersey"               % "jersey-json"               % "1.9",
      "com.sun.jersey.contribs"      % "jersey-guice"              % "1.9",
      "org.fusesource.leveldbjni"    % "leveldbjni-all"            % "1.8",
      "asm"                          % "asm"                       % depend.versions.asm,
      "io.netty"                     % "netty"                     % depend.versions.netty,
      "org.apache.htrace"            % "htrace-core4"              % "4.0.1-incubating"
    )

    // Different versions of these jars have different organizations. Could do
    // something complicated to change old version to new, but for now just
    // keep a list of alternate versions so can exclude both versions
    val alternateVersions = List[ModuleID](
      "org.ow2.asm"                  % "asm"                       % "4.1",
      "org.jboss.netty"              % "netty"                     % "3.2.2.Final",
      "stax"                         % "stax-api"                  % "1.0.1"
    )

    // These jars have classes which interfere with the classes provided by hadoop
    // so we exclude these as well
    val interferingModules = List[ModuleID](
      "org.apache.hadoop"            % "hadoop-mapreduce-client-core"   % depend.versions.hadoopNoMr1,
      "org.apache.hadoop"            % "hadoop-mapreduce-client-common" % depend.versions.hadoopNoMr1,
      "org.apache.hadoop"            % "hadoop-client"                  % depend.versions.hadoopNoMr1
    )

    val exclusions =
      (modules ++ alternateVersions ++ interferingModules)
        .map(m => ExclusionRule(m.organization, m.name))
  }

  @deprecated("""Use exclude("org.mypkg", s"mypkg_${scalaBinaryVersion.value}") instead. This is hard coded to what ever version Scala.binaryVersion is currently""", "1.15.0")
  def sv(module: String): String = s"${module}_${Scala.binaryVersion}"

  object depend {
    object versions {
      // cloudera modules
      def hadoop        = "2.6.0-mr1-cdh5.13.0"
      def hadoopNoMr1   = "2.6.0-cdh5.13.0"
      def parquet       = "1.5.0-cdh5.13.0"
      def parquetFormat = "2.1.0-cdh5.13.0"
      def avro          = "1.7.6-cdh5.13.0"
      def zookeeper     = "3.4.5-cdh5.13.0"
      def jetty         = "6.1.26.cloudera.4"

      // other modules in the hadoop classpath
      def log4j         = "1.2.17"
      def slf4j         = "1.7.5"
      def asm           = "3.2"
      def guava         = "11.0.2"
      def jacksonV1     = "1.8.8"
      def jacksonV2     = "2.3.1"
      def netty         = "3.10.5.Final"

      // cloudera modules *not* on the hadoop classpath
      def hive          = "1.1.0-cdh5.13.0"
      def libthrift     = "0.9.0-cdh5-3"

      // non-hadoop modules
      def macroParadise = "2.1.0"
      def specs         = "3.6"
      def scalaz        = "7.1.1"  // Needs to align with what is required by specs2
      def scalazStream  = "0.7a"   // Needs to align with what is required by specs2
      def shapeless     = "2.2.5"
      def scalacheck    = "1.11.4" // Downgrade to a version that works with both specs2 and scalaz
      def nscalaTime    = "2.10.0"
      def jodaTime      = "2.9.2"  // Needs to align with what is required by nscala-time
      def scalding      = "0.16.0"
      def cascading     = "2.6.1"  // Needs to align with what is required by scalding
      def algebird      = "0.12.0" // Needs to align with what is required by scalding
      def scrooge       = "3.20.0" // Needs to align with what is required by scalding
      def bijection     = "0.9.1"  // Needs to align with what is required by scalding
      def scallop       = "0.9.5"
      def objenesis     = "1.2"
    }

    def omnia(project: String, version: String, configuration: String = "compile"): Seq[ModuleID] =
      Seq("au.com.cba.omnia" %% project % version % configuration)

    def zbi(project: String, version: String, configuration: String = "compile"): Seq[ModuleID] =
      Seq("au.com.cba.zbi" %% project % version % configuration)

    def scaldingproject(
      hadoop: String     = versions.hadoop,
      scalding: String   = versions.scalding,
      algebird: String   = versions.algebird,
      log4j: String      = versions.log4j,
      slf4j: String      = versions.slf4j,
      specs: String      = versions.specs,
      scalacheck: String = versions.scalacheck,
      scalaz: String     = versions.scalaz,
      asm: String        = versions.asm
    ) =
      this.hadoop(hadoop) ++
      this.scalding(scalding, algebird) ++
      this.logging(log4j, slf4j) ++
      this.testing(specs, scalacheck, scalaz, asm)

    /**
      * The modules provided in the hadoop classpath, as provided intransitive dependencies
      *
      * Not a complete list of all modules in the hadoop classpath: just those
      * that appear as dependencies in our software.
      */
    def hadoopClasspath = hadoopCP.modules.map(m => m % "provided" intransitive)

    def hadoop(version: String = versions.hadoop) = Seq(
      "org.apache.hadoop"        % "hadoop-client"                  % version
    ) map noHadoop

    def hive(version: String = versions.hive) = Seq(
      "org.apache.hive"          % "hive-exec"                      % version
    ) map noHadoop

    /** Not a `Seq` since it's a compiler plugin, not a dependency */
    def macroParadise(version: String = versions.macroParadise) =
      "org.scalamacros"          % "paradise"                       % version cross CrossVersion.full

    def scalaz(version: String = versions.scalaz) = Seq(
      "org.scalaz"               %% "scalaz-core"                   % version,
      "org.scalaz"               %% "scalaz-concurrent"             % version
    )

    def scalazStream(version: String = versions.scalazStream) = Seq(
      // Exclude scalaz since the versions are different
      "org.scalaz.stream"        %% "scalaz-stream"                 % version scalaExcludeAll("org.scalaz", "scalaz-core") scalaExcludeAll("org.scalaz", "scalaz-concurrent")
    )

    def shapeless(version: String = versions.shapeless) = Seq(
      "com.chuusai"              %% "shapeless"                     % version
    )

    def testing(
      specs: String = versions.specs, scalacheck: String = versions.scalacheck,
      scalaz: String = versions.scalaz, asm: String = versions.asm,
      configuration: String = "test"
    ) = Seq(
      "org.specs2"               %% "specs2-core"                   % specs       % configuration exclude("org.ow2.asm", "asm"),
      "org.specs2"               %% "specs2-scalacheck"             % specs       % configuration exclude("org.ow2.asm", "asm") scalaExcludeAll("org.scalacheck", "scalacheck"),
      "org.scalacheck"           %% "scalacheck"                    % scalacheck  % configuration scalaExcludeAll("org.scala-lang.modules", "scala-parser-combinators"),
      "org.scalaz"               %% "scalaz-scalacheck-binding"     % scalaz      % configuration scalaExcludeAll("org.scalacheck", "scalacheck"),
      "asm"                      %  "asm"                           % asm         % configuration
    )

    def time(joda: String = versions.jodaTime, nscala: String = versions.nscalaTime) = Seq(
      "joda-time"                %  "joda-time"                     % joda,
      "com.github.nscala-time"   %% "nscala-time"                   % nscala exclude("joda-time", "joda-time")
    )

    def scalding(scalding: String = versions.scalding, algebird: String = versions.algebird, bijection: String = versions.bijection) = Seq(
      noHadoop("com.twitter"     %% "scalding-core"                 % scalding scalaExcludeAll("com.twitter", "bijection-core")),
      "com.twitter"              %% "algebird-core"                 % algebird,
      "com.twitter"              %% "bijection-core"                % bijection
    )

    def logging(log4j: String = versions.log4j, slf4j: String = versions.slf4j) = Seq(
      "log4j"                    %  "log4j"                         % log4j       % "provided",
      "org.slf4j"                %  "slf4j-api"                     % slf4j       % "provided",
      "org.slf4j"                %  "slf4j-log4j12"                 % slf4j       % "provided"
    )

    def scallop(version: String = versions.scallop) = Seq(
      "org.rogach"               %% "scallop"                       % version
    )

    def scrooge(scrooge: String = versions.scrooge, bijection: String = versions.bijection) = Seq(
      "com.twitter"              %% "scrooge-core"                  % scrooge,
      "com.twitter"              %% "bijection-scrooge"             % bijection scalaExcludeAll("com.twitter", "scrooge-core")
    ) map noHadoop

    def parquet(version: String = versions.parquet) = Seq(
      "com.twitter"              % "parquet-cascading"              % version     % "provided"
    ) map noHadoop
  }
}
