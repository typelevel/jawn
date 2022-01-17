ThisBuild / tlBaseVersion := "1.3"
lazy val scala212 = "2.12.15"
lazy val scala213 = "2.13.6"
lazy val scala3 = "3.0.2"
ThisBuild / crossScalaVersions := Seq(scala3, scala213, scala212)
ThisBuild / tlVersionIntroduced := Map("3" -> "1.1.2")
ThisBuild / licenses := Seq("MIT" -> url("http://opensource.org/licenses/MIT"))
ThisBuild / developers += Developer(
  name = "Erik Osheim",
  email = "erik@plastic-idolatry.com",
  id = "d_m",
  url = url("http://github.com/non/")
)

lazy val benchmarkVersion =
  scala212

lazy val jawnSettings = Seq(
  Test / testOptions += Tests.Argument(TestFrameworks.ScalaCheck, "-verbosity", "1"),
  libraryDependencies += "org.scalacheck" %%% "scalacheck" % "1.15.4" % Test
  // scalacOptions ++=
  //   "-deprecation" ::
  //     "-encoding" :: "utf-8" ::
  //     "-feature" ::
  //     "-unchecked" ::
  //     "-Xlint" ::
  //     "-opt:l:method" ::
  //     Nil,
)

lazy val jawnSettingsJVM = List(Test / fork := true)
lazy val jawnSettingsJS = List(
  tlVersionIntroduced := List("2.12", "2.13", "3").map(_ -> "1.2.0").toMap,
  scalaJSLinkerConfig ~= {
    _.withSemantics(
      _.withAsInstanceOfs(org.scalajs.linker.interface.CheckedBehavior.Unchecked)
        .withArrayIndexOutOfBounds(org.scalajs.linker.interface.CheckedBehavior.Unchecked)
    )
  }
)
lazy val jawnSettingsNative = Seq(
  tlVersionIntroduced := List("2.12", "2.13", "3").map(_ -> "1.3.0").toMap,
  crossScalaVersions := List(scala212, scala213)
)

lazy val root = tlCrossRootProject
  .aggregate(parser, util, ast, benchmark)

lazy val parser = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Full)
  .in(file("parser"))
  .settings(name := "parser")
  .settings(moduleName := "jawn-parser")
  .settings(jawnSettings: _*)
  .jvmSettings(jawnSettingsJVM: _*)
  .jsSettings(jawnSettingsJS: _*)
  .nativeSettings(jawnSettingsNative: _*)

lazy val util = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Full)
  .in(file("util"))
  .dependsOn(parser % "compile->compile;test->test")
  .settings(name := "util")
  .settings(moduleName := "jawn-util")
  .settings(jawnSettings: _*)
  .jvmSettings(jawnSettingsJVM: _*)
  .jsSettings(jawnSettingsJS: _*)
  .nativeSettings(jawnSettingsNative: _*)

lazy val ast = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Full)
  .in(file("ast"))
  .dependsOn(parser % "compile->compile;test->test")
  .dependsOn(util % "compile->compile;test->test")
  .settings(name := "ast")
  .settings(moduleName := "jawn-ast")
  .settings(jawnSettings: _*)
  .jvmSettings(jawnSettingsJVM: _*)
  .jsSettings(jawnSettingsJS: _*)
  .nativeSettings(jawnSettingsNative: _*)

lazy val benchmark = project
  .in(file("benchmark"))
  .dependsOn(parser.jvm, util.jvm, ast.jvm)
  .settings(name := "jawn-benchmark")
  .settings(jawnSettings: _*)
  .settings(crossScalaVersions := Seq(benchmarkVersion))
  .enablePlugins(NoPublishPlugin, JmhPlugin)
