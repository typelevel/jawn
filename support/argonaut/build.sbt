name := "argonaut-support"

libraryDependencies ++= Seq(
  "io.argonaut" %% "argonaut" % "6.1" exclude("org.scala-lang", "scala-compiler")
)
