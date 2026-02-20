name := "jawn-benchmarks"

run / javaOptions += "-Xmx6G"

libraryDependencies ++= {
  if (scalaBinaryVersion.value.startsWith("2.12"))
    Seq(
      "io.github.argonaut-io" %% "argonaut" % "6.3.11",
      "io.github.json4s" %% "json4s-native" % "4.1.0",
      "io.github.json4s" %% "json4s-jackson" % "4.1.0",
      "org.playframework" %% "play-json" % "3.0.6",
      "com.rojoma" %% "rojoma-json" % "2.4.3",
      "com.rojoma" %% "rojoma-json-v3" % "3.15.0",
      "io.spray" %% "spray-json" % "1.3.6",
      "org.parboiled" %% "parboiled" % "2.5.1",
      "com.fasterxml.jackson.core" % "jackson-annotations" % "2.21",
      "com.fasterxml.jackson.core" % "jackson-core" % "2.21.0",
      "com.fasterxml.jackson.core" % "jackson-databind" % "2.21.0",
      "com.google.code.gson" % "gson" % "2.13.2"
    )
  else Nil
}

// enable forking in run
run / fork := true
