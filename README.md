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

See https://commbank.github.io/uniform/index.html for the latest version number.

`uniform` helps provide a consistent set of scala settings across projects.

To depend on it, add an entry to `project/plugins.sbt` with an appropriate repository configures:

```
resolvers += Resolver.url("commbank-releases-ivy", new URL("http://commbank.artifactoryonline.com/commbank/ext-releases-local-ivy"))(Patterns("[organization]/[module]_[scalaVersion]_[sbtVersion]/[revision]/[artifact](-[classifier])-[revision].[ext]"))

addSbtPlugin("au.com.cba.omnia" % "uniform-core" % "$VERSION$")
```

Almost all functionality, is included by using the `uniform.project` command.

```
  uniform.project("project-name", "root.package")
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

uniformAssemblySettings
```

`version.sbt`

```
version in ThisBuild := "0.0.1"

uniqueVersionSettings
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

