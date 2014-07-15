import sbt._
import sbt.Keys._

object JawnBuild extends Build {

  override lazy val settings = super.settings ++ Seq(
    organization := "org.spire-math",
    version := "0.5.0",

    scalaVersion := "2.10.4",
    crossScalaVersions := Seq("2.10.4", "2.11.1"),

    scalacOptions ++= Seq(
      "-Yinline-warnings",
      "-deprecation",
      "-optimize",
      "-unchecked"
    ),

    licenses += ("MIT", url("http://opensource.org/licenses/MIT"))
  )

  lazy val noPublish = Seq(
    publish := (),
    publishLocal := (),
    publishArtifact := false)

  lazy val testDeps =
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "2.1.6" % "test",
      "org.scalacheck" %% "scalacheck" % "1.11.4" % "test"
    )

  lazy val parser = Project("parser", file("parser")).
    settings(testDeps)

  lazy val ast = Project("ast", file("ast")).
    settings(testDeps).
    dependsOn(parser)

  lazy val supportArgonaut = Project("support-argonaut", file("support/argonaut")).
    settings(testDeps).
    dependsOn(parser)

  lazy val supportRojoma = Project("support-rojoma", file("support/rojoma")).
    settings(testDeps).
    dependsOn(parser)

  lazy val supportSpray = Project("support-spray", file("support/spray")).
    settings(testDeps).
    dependsOn(parser)

  lazy val benchmark = Project("benchmark", file("benchmark")).
    settings(noPublish: _*).
    dependsOn(ast, supportArgonaut, supportRojoma, supportSpray)

  lazy val root = Project("jawn", file(".")).
    settings(noPublish: _*).
    aggregate(parser, ast, supportArgonaut, supportRojoma, supportSpray)
}
