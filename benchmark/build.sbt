name := "jawn-benchmarks"

run / javaOptions += "-Xmx6G"

libraryDependencies ++= {
  if (scalaBinaryVersion.value.startsWith("2.12"))
    Seq(
      "io.argonaut" %% "argonaut" % "6.3.9",
      "org.json4s" %% "json4s-native" % "4.0.7",
      "org.json4s" %% "json4s-jackson" % "4.0.7",
      "org.playframework" %% "play-json" % "3.0.2",
      "com.rojoma" %% "rojoma-json" % "2.4.3",
      "com.rojoma" %% "rojoma-json-v3" % "3.15.0",
      "io.spray" %% "spray-json" % "1.3.6",
      "org.parboiled" %% "parboiled" % "2.5.1",
      "com.fasterxml.jackson.core" % "jackson-annotations" % "2.16.2",
      "com.fasterxml.jackson.core" % "jackson-core" % "2.16.2",
      "com.fasterxml.jackson.core" % "jackson-databind" % "2.16.2",
      "com.google.code.gson" % "gson" % "2.10.1"
    )
  else Nil
}

// enable forking in run
run / fork := true
