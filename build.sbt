//name := "jawn",

version := "0.4.0"

scalaVersion := "2.10.4"

scalacOptions ++= Seq(
  "-Yinline-warnings",
  "-deprecation",
  "-optimize",
  "-unchecked"
)

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.0" % "test",
  "org.scalacheck" %% "scalacheck" % "1.10.1" % "test"
)
