uniform
=======

[![Build Status](https://travis-ci.org/CommBank/uniform.svg?branch=master)](https://travis-ci.org/CommBank/uniform)
[![Gitter chat](https://badges.gitter.im/CommBank/uniform.png)](https://gitter.im/CommBank/uniform)

```
uniform: remaining the same in all cases and at all times; unchanging in form or character.
```

An sbt plugin for maintaining uniform approach to building cba components.

[Scaladoc](https://commbank.github.io/uniform/latest/api/index.html)

usage
-----

Supported: sbt 0.13.6 or higher (same as [sbt-assembly](https://github.com/sbt/sbt-assembly#using-published-plugin)).

See https://commbank.github.io/uniform/index.html for the latest version number.

`uniform` helps provide a consistent set of scala settings across projects.

To depend on it, add an entry to `project/plugins.sbt` with an appropriate repository configures:

```
resolvers += Resolver.url("commbank-releases-ivy", new URL("http://commbank.artifactoryonline.com/commbank/ext-releases-local-ivy"))(Patterns("[organization]/[module]_[scalaVersion]_[sbtVersion]/[revision]/[artifact](-[classifier])-[revision].[ext]"))

addSbtPlugin("au.com.cba.omnia" % "uniform-core" % "$VERSION$")
```

Almost all functionality, is included by using the `uniform.project` command.

```
  uniform.project("project-name", "project.root.package")
```

A concrete example might be:
```
  uniform.project("thermometer", "au.com.cba.omnia.thermometer")
```

The only additional thing to include is the unique version number plugin for
generating sane versioned components as opposed to snapshots and their ilk.

Versioning should be done in a `version.sbt` file in the root of a project, and
it should look like this:

```
version in ThisBuild := "0.0.1"

localVersionSettings
```

This will append `-SNAPSHOT` to the version number. The CI builds overwrite the version with a unique version at build time.

Uniform’s core [VersionInfoPlugin](https://github.com/CommBank/uniform/blob/master/uniform-core/src/main/scala/au/com/cba/omnia/uniform/core/version/VersionInfoPlugin.scala) generates a Scala object named `VersionInfo` in your `project.root.package` with the build’s version number, Git commit and date strings (see complete example below).

`uniform` flavours provide additional pre-canned configs for `assembly`, `thrift` and consistent dependency versions.

a complete example
------------------

`project/plugins.sbt`

```
resolvers += Resolver.url("commbank-releases-ivy", new URL("http://commbank.artifactoryonline.com/commbank/ext-releases-local-ivy"))(Patterns("[organization]/[module]_[scalaVersion]_[sbtVersion]/[revision]/[artifact](-[classifier])-[revision].[ext]"))

addSbtPlugin("au.com.cba.omnia" % "uniform-core" % "$VERSION$")

addSbtPlugin("au.com.cba.omnia" % "uniform-thrift" % "$VERSION$")

addSbtPlugin("au.com.cba.omnia" % "uniform-assembly" % "$VERSION$")

addSbtPlugin("au.com.cba.omnia" % "uniform-dependency" % "$VERSION$")
```

`build.sbt`

```
uniform.project("project-name", "root.package")

uniformThriftSettings

uniformAssemblySettings(splitPackageDeps = true)
```

`version.sbt`

```
version in ThisBuild := "0.0.1"

localVersionSettings
```

`src/main/scala/project/root/package/Main.scala`

```
package project.root.package

object Main extends App {
  println(s"=== ${getClass.getSimpleName} === ${VersionInfo.verbose} ===")
}
```

`sbt run`

```
=== Main$ === 0.0.1-SNAPSHOT-20161219024858-1afb454 ===
```

assemblies
----------

By default, the `assembly` task creates a fat JAR of your project with all its dependencies.
In the past, split JARs have only been possible using `assemblyPackageDependency` and `assemblyOption`
(see [sbt-assembly: Splitting your project and deps JARs](https://github.com/sbt/sbt-assembly#splitting-your-project-and-deps-jars)).
As of version 1.13.0 (December 2016), `uniform-assembly` supports a convenience option for doing this with `uniformAssemblySettings`
(see [UniformAssemblyPlugin.scala](https://github.com/CommBank/uniform/blob/master/uniform-assembly/src/main/scala/au/com/cba/omnia/uniform/assembly/UniformAssemblyPlugin.scala) for details):


| Configuration in `build.sbt`                         | `assembly` output                              | `assemblyPackageDependency` output |
| ---------------------------------------------------- | ---------------------------------------------- | ---------------------------------- |
| `uniformAssemblySettings` (past and current default) | Fat jar with project and dependencies (`.jar`) | Dependencies only (`-deps.jar`)    |
| `uniformAssemblySettings(splitPackageDeps = false)`  | As above                                       | As above                           |
| `uniformAssemblySettings(splitPackageDeps = true)`   | Project only (`-thin.jar`)                     | As above                           |

Default usage with `uniformAssemblySettings(splitPackageDeps = false)`:

    sbt> assembly
    ...
    [info] Packaging ./target/scala-2.11/project-name-assembly-0.0.1-SNAPSHOT.jar ...
    ...
    sbt> assemblyPackageDependency
    ...
    [info] Packaging ./target/scala-2.11/project-name-assembly-0.0.1-SNAPSHOT-deps.jar ...

Optional usage with `uniformAssemblySettings(splitPackageDeps = true)`:

    sbt> assembly
    ...
    [info] Packaging ./target/scala-2.11/project-name_2.11-0.0.1-SNAPSHOT-thin.jar ...
    ...
    sbt> assemblyPackageDependency
    ...
    [info] Packaging ./target/scala-2.11/project-name_2.11-0.0.1-SNAPSHOT-deps.jar ...

In this case, the `deps` jar can be passed to [Hadoop’s `-libjars`](https://hadoop.apache.org/docs/r2.6.0/hadoop-project-dist/hadoop-common/CommandsManual.html#Generic_Options) option to avoid unjarring the dependencies in a traditional fat assembly.

### Sharing assemblyPackageDependency deps.jar between subprojects

If you are using splitPackageDeps in a repo with many subprojects sharing the same libraryDependencies,
you can re-use the deps.jar (instead of independently re-building it in each subproject).
This can save time and disk usage during the builds.
As of version 2.3.0 (December 2018), `uniform-assembly` supports a convenience method for this:

`build.sbt`

```
...
lazy val all = (project in file(".")).settings(
  ...
).aggregate(sub1, sub2)

lazy val sub1 = project in file("sub1")

lazy val sub2 = shareSplitAssemblyPackageDependencyJar(project in file("sub2"), sub1)
...
```

docs
----

`uniform.docSettings` and `uniform.ghsettings` configure the `sbt-site` and `sbt-unidoc` projects to
create documentation for publishing under `target/site`. It will take the content from `src/site`
and also use `unidoc` to generate unified api documentation for the whole project.


`project/build.scala`

```
...
lazy val all = Project(
    id = "all"
  , base = file(".")
  , settings = uniform.project("test", "au.com.cba.omnia.test") ++
      uniform.ghsettings ++ uniform.docSettings("https://commbank.github.io/test/latest/api")
  , aggregate = Seq(sub1)
  )

lazy val sub1 = Project(
    id = "sub1"
  , base = file("sub1")
  , settings = uniform.project("test-sub1", "au.com.cba.omnia.test.sub1") ++
      uniform.docSettings("https://commbank.github.io/test/latest/api")
  )
...
```

