import sbt._
import sbt.Keys._

import org.typelevel.sbt.Developer
import org.typelevel.sbt.TypelevelPlugin._

object JawnBuild extends Build {

  lazy val standardSettings = Seq(
    organization := "org.spire-math",

    scalaVersion := "2.11.4",
    crossScalaVersions := Seq("2.10.4", "2.11.4"),

    homepage := Some(url("http://github.com/non/jawn/")),

    scalacOptions ++= Seq(
      "-Yinline-warnings",
      "-deprecation",
      "-optimize",
      "-unchecked"
    ),

    licenses += ("MIT", url("http://opensource.org/licenses/MIT")),

    resolvers += Resolver.sonatypeRepo("releases")
  ) ++ typelevelDefaultSettings ++ Seq(
    TypelevelKeys.signArtifacts := true,
    TypelevelKeys.githubDevs += Developer("Erik Osheim", "non"),
    TypelevelKeys.githubProject := ("non", "jawn")
  )

  lazy val noPublish = Seq(
    publish := (),
    publishLocal := (),
    publishArtifact := false
  )

  lazy val testDeps = Seq(
    "org.scalatest" %% "scalatest" % "2.2.1" % "test",
    "org.scalacheck" %% "scalacheck" % "1.11.6" % "test"
  )

  lazy val parser = Project(
    id = "parser",
    base = file("parser"),
    settings = standardSettings ++ Seq(
      libraryDependencies ++= testDeps
    )
  )

  lazy val ast = Project(
    id = "ast",
    base = file("ast"),
    settings = standardSettings ++ Seq(
      libraryDependencies ++= testDeps
    ),
    dependencies = Seq(parser)
  )

  def support(name: String) = Project(
    id = name,
    base = file(s"support/$name"),
    settings = standardSettings ++ Seq(
      libraryDependencies ++= testDeps
    ),
    dependencies = Seq(parser)
  )

  lazy val supportArgonaut = support("argonaut")
  lazy val supportJson4s = support("json4s")
  lazy val supportPlay = support("play")
  lazy val supportRojoma = support("rojoma")
  lazy val supportRojomaV3 = support("rojoma-v3")
  lazy val supportSpray = support("spray")

  lazy val all =
    Seq(parser, ast, supportArgonaut, supportJson4s, supportPlay, supportRojoma, supportRojomaV3, supportSpray)

  lazy val benchmark = Project(
    id = "benchmark",
    base = file("benchmark"),
    settings = standardSettings ++ noPublish,
    dependencies = all.map(Project.classpathDependency[Project])
  )

  lazy val root = Project(
    id = "jawn",
    base = file("."),
    settings = standardSettings ++ noPublish,
    aggregate = all.map(Project.projectToRef)
  )

}
