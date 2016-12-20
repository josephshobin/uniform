---
layout: default
title: Uniform
---

Uniform
=======

An sbt plugin for maintaining uniform approach to building cba components.


* [Readme](https://github.com/CommBank/uniform/)
* [Scaladoc](/uniform/latest/api/index.html)

The latest version is `{{ site.releaseVersion }}`.

Usage
-----

Add the following to your plugins.sbt, including or excluding plugins as needed.

{% highlight scala %}
val uniformVersion = "{{ site.releaseVersion }}"

addSbtPlugin("au.com.cba.omnia" % "uniform-core"       % uniformVersion)

addSbtPlugin("au.com.cba.omnia" % "uniform-dependency" % uniformVersion)

addSbtPlugin("au.com.cba.omnia" % "uniform-thrift"     % uniformVersion)

addSbtPlugin("au.com.cba.omnia" % "uniform-assembly"   % uniformVersion)
{% endhighlight %}
