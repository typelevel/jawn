import sbt._
import sbt.Keys._

object JawnBuild extends Build {

  override lazy val settings = super.settings ++ Seq(
    organization := "org.jsawn",
    version := "0.5.5",

    scalaVersion := "2.10.4",
    crossScalaVersions := Seq("2.10.4", "2.11.1"),

    scalacOptions ++= Seq(
      "-Yinline-warnings",
      "-deprecation",
      "-optimize",
      "-unchecked"
    ),

    licenses += ("MIT", url("http://opensource.org/licenses/MIT")),

    resolvers ++= Seq(
      "mth.io snapshots" at "http://repo.mth.io/snapshots",
      "mth.io releases" at "http://repo.mth.io/releases",
      "snapshots" at "http://oss.sonatype.org/content/repositories/snapshots",
      "releases"  at "http://oss.sonatype.org/content/repositories/releases",
      "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"
    )
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

  lazy val supportJson4s = Project("support-json4s", file("support/json4s")).
    settings(testDeps).
    dependsOn(parser)

  lazy val supportPlay = Project("support-play", file("support/play")).
    settings(testDeps).
    dependsOn(parser)

  lazy val supportRojoma = Project("support-rojoma", file("support/rojoma")).
    settings(testDeps).
    dependsOn(parser)

  lazy val supportRojomaV3 = Project("support-rojoma-v3", file("support/rojoma-v3")).
    settings(testDeps).
    dependsOn(parser)

  lazy val supportSpray = Project("support-spray", file("support/spray")).
    settings(testDeps).
    dependsOn(parser)

  lazy val benchmark = Project("benchmark", file("benchmark")).
    settings(noPublish: _*).
    dependsOn(parser, ast, supportArgonaut, supportJson4s, supportPlay, supportRojoma, supportRojomaV3, supportSpray)


  lazy val root = Project("jawn", file(".")).
    settings(noPublish: _*).
    aggregate(parser, ast, supportArgonaut, supportJson4s, supportPlay, supportRojoma, supportSpray)
}
