import ReleaseTransformations._

lazy val previousJawnVersion = "0.11.1"

lazy val stableCrossVersions =
  Seq("2.10.7", "2.11.12", "2.12.6")

// we'll support 2.13.0-M1 soon but not yet
lazy val allCrossVersions =
  stableCrossVersions :+ "2.13.0-M4"

lazy val benchmarkVersion =
  "2.12.6"

lazy val jawnSettings = Seq(
  organization := "org.spire-math",
  scalaVersion := "2.12.6",

  //crossScalaVersions := allCrossVersions,
  crossScalaVersions := stableCrossVersions,

  mimaPreviousArtifacts := Set(organization.value %% moduleName.value % previousJawnVersion),

  resolvers += Resolver.sonatypeRepo("releases"),

  libraryDependencies += {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, v)) if v < 13 =>
        "org.scalatest" %% "scalatest" % "3.0.5" % Test
      case _ =>
        "org.scalatest" %% "scalatest" % "3.0.6-SNAP1" % Test
    }
  },

  libraryDependencies ++=
    "org.scalacheck" %% "scalacheck" % "1.14.0" % Test ::
    Nil,

  scalacOptions ++=
    "-deprecation" ::
    "-encoding" :: "utf-8" ::
    "-feature" ::
    "-unchecked" ::
    "-Xfatal-warnings" ::
    "-Xfuture" ::
    Nil,

  scalacOptions += {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, v)) if v <= 11 =>
        "-optimize"
      case _ =>
        "-opt:l:method"
    }
  },

  licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
  homepage := Some(url("http://github.com/non/jawn")),

  // release stuff
  releaseCrossBuild := true,
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := Function.const(false),

  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value) {
      Some("Snapshots" at nexus + "content/repositories/snapshots")
    } else {
      Some("Releases" at nexus + "service/local/staging/deploy/maven2")
    }
  },

  scmInfo := Some(ScmInfo(
    browseUrl = url("https://github.com/non/jawn"),
    connection = "scm:git:git@github.com:non/jawn.git"
  )),

  developers += Developer(
    name = "Erik Osheim",
    email = "erik@plastic-idolatry.com",
    id = "d_m",
    url = url("http://github.com/non/")
  ),

  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    releaseStepCommandAndRemaining("+test"), // formerly runTest
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    releaseStepCommandAndRemaining("+publishSigned"),
    setNextVersion,
    commitNextVersion,
    releaseStepCommandAndRemaining("sonatypeReleaseAll"),
    pushChanges))

lazy val noPublish = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false,
  mimaPreviousArtifacts := Set())

lazy val root = project.in(file("."))
  .aggregate(all.map(Project.projectToRef): _*)
  .disablePlugins(JmhPlugin)
  .settings(name := "jawn")
  .settings(jawnSettings: _*)
  .settings(noPublish: _*)

lazy val parser = project.in(file("parser"))
  .settings(name := "parser")
  .settings(moduleName := "jawn-parser")
  .settings(jawnSettings: _*)
  .settings(crossScalaVersions := allCrossVersions)
  .disablePlugins(JmhPlugin)

lazy val util = project.in(file("util"))
  .dependsOn(parser % "compile->compile;test->test")
  .settings(name := "util")
  .settings(moduleName := "jawn-util")
  .settings(jawnSettings: _*)
  .settings(crossScalaVersions := allCrossVersions)
  .disablePlugins(JmhPlugin)

lazy val ast = project.in(file("ast"))
  .dependsOn(parser % "compile->compile;test->test")
  .dependsOn(util % "compile->compile;test->test")
  .settings(name := "ast")
  .settings(moduleName := "jawn-ast")
  .settings(jawnSettings: _*)
  .settings(crossScalaVersions := allCrossVersions)
  .disablePlugins(JmhPlugin)

def support(s: String) =
  Project(id = s, base = file(s"support/$s"))
    .settings(name := (s + "-support"))
    .settings(moduleName := "jawn-" + s)
    .dependsOn(parser)
    .settings(jawnSettings: _*)
    .disablePlugins(JmhPlugin)

lazy val supportArgonaut = support("argonaut")
  .settings(crossScalaVersions := stableCrossVersions)
  .settings(libraryDependencies += "io.argonaut" %% "argonaut" % "6.2.2")

lazy val supportJson4s = support("json4s")
  .dependsOn(util)
  .settings(crossScalaVersions := allCrossVersions)
  .settings(libraryDependencies += "org.json4s" %% "json4s-ast" % "3.6.0")

lazy val supportPlay = support("play")
  .settings(crossScalaVersions := stableCrossVersions)
  .settings(libraryDependencies += {
    "com.typesafe.play" %% "play-json" % "2.6.9"
  })

lazy val supportRojoma = support("rojoma")
  .settings(crossScalaVersions := stableCrossVersions)
  .settings(libraryDependencies += "com.rojoma" %% "rojoma-json" % "2.4.3")

lazy val supportRojomaV3 = support("rojoma-v3")
  .settings(crossScalaVersions := stableCrossVersions)
  .settings(libraryDependencies += "com.rojoma" %% "rojoma-json-v3" % "3.8.0")

lazy val supportSpray = support("spray")
  .settings(crossScalaVersions := allCrossVersions)
  .settings(libraryDependencies += "io.spray" %% "spray-json" % "1.3.4")

lazy val benchmark = project.in(file("benchmark"))
  .dependsOn(all.map(Project.classpathDependency[Project]): _*)
  .settings(name := "jawn-benchmark")
  .settings(jawnSettings: _*)
  .settings(scalaVersion := benchmarkVersion)
  .settings(crossScalaVersions := Seq(benchmarkVersion))
  .settings(noPublish: _*)
  .enablePlugins(JmhPlugin)

lazy val all =
  Seq(parser, util, ast, supportArgonaut, supportJson4s, supportPlay, supportRojoma, supportRojomaV3, supportSpray)
