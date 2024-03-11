ThisBuild / tlBaseVersion := "1.5"
lazy val scala212 = "2.12.19"
lazy val scala213 = "2.13.13"
lazy val scala3 = "3.3.3"
ThisBuild / crossScalaVersions := Seq(scala3, scala213, scala212)
ThisBuild / tlVersionIntroduced := Map("3" -> "1.1.2")
ThisBuild / startYear := Some(2012)
ThisBuild / licenses := Seq("MIT" -> url("http://opensource.org/licenses/MIT"))
ThisBuild / developers += Developer(
  name = "Erik Osheim",
  email = "erik@plastic-idolatry.com",
  id = "d_m",
  url = url("http://github.com/non/")
)
ThisBuild / tlFatalWarnings := false

lazy val jawnSettings = Seq(
  Test / testOptions += Tests.Argument(TestFrameworks.ScalaCheck, "-verbosity", "1"),
  libraryDependencies += "org.scalacheck" %%% "scalacheck" % "1.17.0" % Test
)

lazy val jawnSettingsJVM = List(Test / fork := true)
lazy val jawnSettingsJS = List(
  tlVersionIntroduced := List("2.12", "2.13", "3").map(_ -> "1.2.0").toMap
)
lazy val jawnSettingsNative = Seq(
  tlVersionIntroduced := Map(
    "2.12" -> "1.3.0",
    "2.13" -> "1.3.0",
    "3" -> "1.4.0"
  )
)

lazy val root = tlCrossRootProject
  .aggregate(parser, util, ast, benchmark)
  .settings(
    name := "jawn-root"
  )

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
  .enablePlugins(NoPublishPlugin, JmhPlugin)
