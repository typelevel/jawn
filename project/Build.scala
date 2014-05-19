import sbt._
import sbt.Keys._

object MyBuild extends Build {
  lazy val root = Project("jawn", file("."))
  lazy val parser: Project = Project("parser", file("parser")).dependsOn(root)
  lazy val ast: Project = Project("ast", file("ast")).dependsOn(root)
  lazy val benchmark: Project = Project("benchmark", file("benchmark")).dependsOn(root)
}
