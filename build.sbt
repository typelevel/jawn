import ReleaseTransformations._

lazy val previousJawnVersion = "0.14.0"

lazy val scala212 = "2.12.10"
lazy val scala213 = "2.13.1"
lazy val dotty = "0.21.0-RC1"
ThisBuild / scalaVersion := scala212
ThisBuild / organization := "org.typelevel"
ThisBuild / licenses += ("MIT", url("http://opensource.org/licenses/MIT"))
ThisBuild / homepage := Some(url("http://github.com/typelevel/jawn"))

ThisBuild / scmInfo := Some(
  ScmInfo(
    browseUrl = url("https://github.com/typelevel/jawn"),
    connection = "scm:git:git@github.com:typelevel/jawn.git"
  )
)

ThisBuild / developers += Developer(
  name = "Erik Osheim",
  email = "erik@plastic-idolatry.com",
  id = "d_m",
  url = url("http://github.com/non/")
)

lazy val benchmarkVersion =
  scala212

lazy val jawnSettings = Seq(
  crossScalaVersions := Seq(scala212, scala213, dotty),
  mimaPreviousArtifacts := Set(organization.value %% moduleName.value % previousJawnVersion),
  resolvers += Resolver.sonatypeRepo("releases"),
  Test / fork := true,
  testOptions in Test += Tests.Argument(TestFrameworks.ScalaCheck, "-verbosity", "1"),
  libraryDependencies += ("org.scalacheck" %% "scalacheck" % "1.14.3" % Test).withDottyCompat(scalaVersion.value),
  libraryDependencies ++= (
    if (isDotty.value) Nil
    else List("org.typelevel" %% "claimant" % "0.1.2" % Test)
  ),
  scalacOptions ++=
    "-deprecation" ::
      "-encoding" :: "utf-8" ::
      "-feature" ::
      "-unchecked" ::
      Nil,
  scalacOptions ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, v)) if v <= 12 =>
        Seq("-Xfatal-warnings", "-Xfuture")
      case _ =>
        Nil
    }
  },
  scalacOptions += {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, v)) if v <= 11 =>
        "-optimize"
      case _ =>
        "-opt:l:method"
    }
  },
  // release stuff
  releaseCrossBuild := true,
  publishMavenStyle := true,
  publishArtifact in Test := false,
  publishArtifact in (Compile, packageDoc) := !isDotty.value,
  pomIncludeRepository := Function.const(false),
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value) {
      Some("Snapshots".at(nexus + "content/repositories/snapshots"))
    } else {
      Some("Releases".at(nexus + "service/local/staging/deploy/maven2"))
    }
  },
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
    pushChanges
  )
)

lazy val noPublish = Seq(publish / skip := true, mimaPreviousArtifacts := Set())

lazy val root = project
  .in(file("."))
  .aggregate(all.map(Project.projectToRef): _*)
  .disablePlugins(JmhPlugin)
  .settings(name := "jawn")
  .settings(jawnSettings: _*)
  .settings(crossScalaVersions := List())
  .settings(noPublish: _*)

lazy val parser = project
  .in(file("parser"))
  .settings(name := "parser")
  .settings(moduleName := "jawn-parser")
  .settings(jawnSettings: _*)
  .settings(
    Test / unmanagedSourceDirectories ++= (
      if (isDotty.value) {
        List(baseDirectory.value / "src" / "test" / "dotty")
      } else Nil
    )
  )
  .disablePlugins(JmhPlugin)

lazy val util = project
  .in(file("util"))
  .dependsOn(parser % "compile->compile;test->test")
  .settings(name := "util")
  .settings(moduleName := "jawn-util")
  .settings(jawnSettings: _*)
  .disablePlugins(JmhPlugin)

lazy val ast = project
  .in(file("ast"))
  .dependsOn(parser % "compile->compile;test->test")
  .dependsOn(util % "compile->compile;test->test")
  .settings(name := "ast")
  .settings(moduleName := "jawn-ast")
  .settings(jawnSettings: _*)
  .disablePlugins(JmhPlugin)

def support(s: String) =
  Project(id = s, base = file(s"support/$s"))
    .settings(name := (s + "-support"))
    .settings(moduleName := "jawn-" + s)
    .dependsOn(parser)
    .settings(jawnSettings: _*)
    .disablePlugins(JmhPlugin)

lazy val supportJson4s = support("json4s")
  .dependsOn(util)
  .settings(libraryDependencies += ("org.json4s" %% "json4s-ast" % "3.6.7").withDottyCompat(scalaVersion.value))

lazy val supportPlay = support("play")
  .settings(libraryDependencies += ("com.typesafe.play" %% "play-json" % "2.8.1").withDottyCompat(scalaVersion.value))

lazy val supportSpray = support("spray")
  .settings(libraryDependencies += ("io.spray" %% "spray-json" % "1.3.5").withDottyCompat(scalaVersion.value))

lazy val benchmark = project
  .in(file("benchmark"))
  .dependsOn(all.map(Project.classpathDependency[Project]): _*)
  .settings(name := "jawn-benchmark")
  .settings(jawnSettings: _*)
  .settings(scalaVersion := benchmarkVersion)
  .settings(crossScalaVersions := Seq(benchmarkVersion))
  .settings(noPublish: _*)
  .enablePlugins(JmhPlugin)

lazy val all =
  Seq(parser, util, ast, supportJson4s, supportPlay, supportSpray)
