name := "argonaut-support"

libraryDependencies ++= Seq(
  "io.argonaut" %% "argonaut" % "6.1-M6" exclude("org.scala-lang", "scala-compiler")
)
