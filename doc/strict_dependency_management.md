# Strict conflict management

Uniform-dependency contains a `strictDependencySettings` setting which you can add to your build,
causing Ivy to use strict conflict management, resulting in build failures if you depend on
conflicting versions of any library. This document describes methods to debug and fix conflicts.

The rest of uniform's documentation is available [here][uniform].

## Terminology, briefly

In this document, the words "library" or "module" refers to a single body of code packaged together,
what Ivy would call a "module", regardless of it's version. Sometimes libraries will change details
over their lifetime (e.g., later versions of `asm.asm` became `ow.org2.asm.asm`). This document
refers to these as the same module, even though sbt and Ivy consider them separate.

The word "dependency" refers to modules which you explicitly depend on in your sbt file.
Dependencies are the roots of the forest of all the modules your project depends on.

## Summary

  -  To resolve conflict issues, you must debug and fix all conflicts.
  -  To debug conflicts:
     -  Temporarily disable strict conflict management to get dependency information.
     -  Look at Ivy report files or use the sbt-dependency-graph plugin to view dependency
        information and find the dependencies which result in the conflict.
  -  To fix conflicts:
     -  Use `noHadoop` and `depend.hadoopClasspath` to resolve conflicts on libraries in the
        hadoop classpath.
     -  Adjust dependency versions or use exclude statements to fix conflicts in other libraries.
     -  Avoid dependency overrides where possible.

## A brief overview of sbt, Ivy, and conflict management

In your sbt file you will specify a list of dependencies. When you ask sbt to find the transitive
forest of all the modules you depend on, sbt:

  1. Creates some [Ivy files][ivy_files] describing your configurations and dependency list. These
     files are located in the `target/resolution-cache/<organization>/<name>/<version-specific-path>/`
     directory.
  2. Asks Ivy to resolve your dependencies.
  3. Ivy finds the transitive list of all modules you transitively depend on.
  4. Ivy [resolves conflicting versions][conflict] of these modules, using the specified conflict
     manager. The default conflict manager picks the version it guesses is the "latest".
  5. Ivy creates files listing all the modules you transitvely depend on, and the dependency
     information between them. There is one of these files for each Ivy [configuration][conf],
     located in the `target/resolution-cache/reports/` directory.
  6. Sbt translates Ivy's output into sbt's own data structures, and continues with the build.

When you have [strict conflict management][conflict_managers] enabled, Ivy simply throws an
exception upon the first conflict it finds. This means that:

  -  Sbt does not get any module list or dependency information back from Ivy.
  -  Ivy only reports one conflict at a time.
  -  Before you can continue with the build, there must be no conflicts.

Be aware that a module may change it's details over it's lifetime (e.g `asm.asm` became
`org.ow2.asm.asm`). In this case Ivy won't pick up the conflict, because it thinks these are two
separate modules.

## Debugging conflicts

In order to build with strict confict management, you must resolve any conflicts among the libraries
you depend on. To resolve a conflict, you must find the dependencies which introduce conflicting
versions, and tweak the dependencies to avoid the conflict. This document describes two methods to
find the dependencies which introduce a conflict:

  1. Viewing the raw Ivy report files.
  2. Using the sbt-dependency-graph plugin.

### Interpreting raw Ivy report files

In order to debug conflicts, you need the dependency information from Ivy. A slow but reliable way of
debugging conflicts is to directly view Ivy's output.

When the strict conflict manager fails, it throws an exception and no Ivy report files are generated.
You will need to disable the strict conflict manager using `conflictManager := ConflictManager.default`
before you can debug any conflicts.

Ivy dumps it's output to Ivy report files in the `target/resolution-cache/reports/` directory. There
is one report file per configuration. The Ivy report files contain a list of `module` declarations.
We are interested in the revision and caller information inside each `module` declaration:

    <module organisation="cascading" name="cascading-core">
      <revision name="2.5.5" ... >
        ...
        <caller organisation="cascading"   name="cascading-local"    ... rev="2.5.5" rev-constraint-default="2.5.5" rev-constraint-dynamic="2.5.5" callerrev="2.5.5"/>
        <caller organisation="cascading"   name="cascading-hadoop"   ... rev="2.5.5" rev-constraint-default="2.5.5" rev-constraint-dynamic="2.5.5" callerrev="2.5.5"/>
        <caller organisation="com.twitter" name="scalding-core_2.10" ... rev="2.5.5" rev-constraint-default="2.5.5" rev-constraint-dynamic="2.5.5" callerrev="0.12.0"/>
        ...
      </revision>
    </module>

Each `module` contains a list of `revision`s. Evicted revisions are marked as such. Each revision contains
a list of the modules which depend on that revision. You can trace callers back from the conflicting
module to build up a dependency graph, culminating in the dependencies which introduce the conflicts.

Some notes:

  -  When a module `A` depends on `B-v1`, which gets evicted in favour of `B-v2`, Ivy keeps the original
     dependency from `A` to `B-v1`, and adds an additional dependency from `A` to `B-v2`. See a real
     example below.
  -  The `rev-constraint-dynamic` field in each `caller` declaration appears to show the revision that
     the caller originally depended on, although the Ivy documentation does not appear to support this.
  -  You may use these files to view conflicts in hidden configurations, such as `compile-internal`,
     which the user can't access via sbt.

Example of an extra dependency being added from `hive-ant` to `velocity-1.7`, in addition to `hive-ant`s
original dependency on `velocity-1.5`:


    <module organisation="org.apache.velocity" name="velocity">
      <revision name="1.7" ... >
        ...
        <caller organisation="org.apache.hive" name="hive-ant" ... rev="1.5" ... />
        <caller organisation="org.apache.avro" name="avro-ipc" ... rev="1.7" ... />
        ...
      </revision>
      <revision name="1.5" ... >
        ...
        <evicted-by rev="1.7"/>
        <caller organisation="org.apache.hive" name="hive-ant" ... rev="1.5" ... />
      </revision>
    </module>

### Viewing dependency information via sbt-dependency-graph

Manually tracing callers in the Ivy report files is reliable, but time-consuming. [sbt-dependency-graph][sbt_dep_graph]
is an sbt plugin which can read that information and generate graphs and trees for you.

You can install sbt-dependency-graph globally to avoid having to install it in each repo you worl with:

    $ cat ~/.sbt/<version>/plugins/plugins.sbt
    addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.7.4")
    $ cat ~/.sbt/<version>/global.sbt
    net.virtualvoid.sbt.graph.Plugin.graphSettings

While running concurrently, sbt can sometimes delete the Ivy report files while sbt-dependency-graph is
trying to read them. You can force sbt to run one task at a time to avoid this:

    $ cat <project>/serial.sbt
    concurrentRestrictions := Seq(Tags.limit(Tags.CPU, 1), Tags.limitAll(1))

As before, to have any Ivy repory files for sbt-dependency-graph to work with, you will need to disable
the strict conflict manager using `conflictManager := ConflictManager.default`.

sbt-dependency-graph provides many functions to look at dependencies. `whatDependsOn <organization> <name> <version>`
shows the transitive set of libraries which depended on the specified module. Use `whatDependsOn` on
conflicting libraries to see which dependencies in which projects caused the conflict.

Some notes:

  -  Remember that when a module `A` depends on `B-v1`, which gets evicted in favour of `B-v2`, Ivy keeps
     the original dependency from `A` to `B-v1`, and adds an additional dependency from `A` to `B-v2`.
  -  Remember that scala libraries have an extra `_<scala_binary_version>` to their name (e.g. `scalacheck`
     may really be `scalacheck_2.10`).
  -  `whatDependsOn` runs on one configuration at a time. You may need to search for the dependency in
     `compile`, `test`, and `provided` configurations before finding the conflicting libraries. If the
     conflict is in a hidden internal configuration you will not be able to see it within sbt.
  -  sbt-dependency-graph re-runs Ivy every time you call one of it's functions. This becomes time-consuming.

## Fixing conflicts

Once you have identified the dependencies which cause a conflict, you need to adjust the dependencies
to remove the conflict. This document describes four methods to remove conflicts:

  1. Use `noHadoop` to avoid conflicts on hadoop classpath libraries.
  2. Adjust dependency versions so that the dependencies agree on the version to use.
  3. Pick the dependency with the version to keep, and exclude the conflicting module from the other dependencies.
  4. Use `dependencyOverrides` to force a module to have the specified version.

### Conflicts on hadoop classpath modules

The libraries on the hadoop classpath are provided at runtime, and our projects do not get to choose the
version of those libraries. Uniform-dependency exports a `noHadoop` function which excludes hadoop classpath
libraries from a dependency. You can then include the correct versions of the hadoop classpath libraries by
adding `depend.hadoopClasspath` to your dependencies.

For instance:

    depend.hadoopClasspath ++ Seq(
      noHadoop("org.apache.sqoop" % "sqoop" % "1.4.5-cdh5.2.4")
    )

`depend.hadoopClasspath` adds the hadoop classpath modules into the `provided` classpath, so the hadoop
classpath modules will not be added to your packages.

### Adjusting dependency versions

Sometimes, particularly when depending on related libraries, it is possible to simply adjust the dependency
version so that the dependencies depend on the same version of the conflicting module. This has the advantage
that both dependencies are tested on the version of the module included in production, and you get alerted if
you update either dependency and the conflict reappears.

### Exclude module from dependencies with bad versions

You can exclude a module from the transitive graph of modules pulled in by a particular dependency. For
example, if you were to include both specs2 and scalacheck, you might need to exclude scalacheck from specs2:

    "org.scalacheck" %% "scalacheck" % depend.versions.scalacheck,
    "org.specs2"     %% "specs2"     % depend.versions.specs exclude("org.scalacheck", "scalacheck_2.10")

This is not modular: the two dependencies are no longer independent, but it has the advantage that that the
version of the conflicting module will automatically track the version pulled in by the winning dependency.

### Dependency overrides

Inside sbt, we sometimes use dependency overrides to pin the version of a conflicting module:

    // conflict between hive-exec / sqoop vs. avro-mapred
    dependencyOverrides += "org.apache.velocity" % "velocity"  % "1.7",

Dependency overrides are a poor way to resolve conflicts. The conflicting module will stay pinned at the
specified version, even if the dependencies which introduce the conflict switch to different versions.

## Miscellaneous notes

  -  You may exclude libraries which aren't required to compile your solution, but are required to run it.
     Hopefully your tests will fail if you do this.
  -  If you haven't included all the libraries you need, you may get a `NoClassDefFound` exception. This error
     might not point to the right class. Check the cause of the exception as well.
  -  Be carefull not to mix hadoop MR1 and hadoop MR2 classes on the same classspath. The classes share the
     same name, but cascading/scalding/us assume MR1 classes, and won't work with MR2 classes.
  -  The `dependencyClasspath` sbt setting shows the final classpath used to compile your project.
  -  The sbt commands `inspect` and `show` and the sbt source code are very helpful.
  -  sbt does not support custom conflict managers.

[uniform]:           https://github.com/CommBank/uniform/blob/master/src/site/index.md                  "Uniform"
[ivy_files]:         http://ant.apache.org/ivy/history/latest-milestone/ivyfile.html                    "Ivy Files"
[conflict]:          http://ant.apache.org/ivy/history/latest-milestone/ivyfile/conflict.html           "Conflict"
[conflict_managers]: http://ant.apache.org/ivy/history/latest-milestone/settings/conflict-managers.html "Conflict Managers"
[conf]:              http://ant.apache.org/ivy/history/latest-milestone/ivyfile/conf.html               "Conf"
[sbt_dep_graph]:     https://github.com/jrudolph/sbt-dependency-graph                                   "sbt-dependency-graph"
