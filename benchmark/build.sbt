name := "jawn-benchmarks"

run / javaOptions += "-Xmx6G"

libraryDependencies ++= {
  if (scalaBinaryVersion.value.startsWith("2.12"))
    Seq(
      "io.argonaut" %% "argonaut" % "6.2.5",
      "org.json4s" %% "json4s-native" % "3.5.4",
      "org.json4s" %% "json4s-jackson" % "3.5.4",
      "com.typesafe.play" %% "play-json" % "2.6.14",
      "com.rojoma" %% "rojoma-json" % "2.4.3",
      "com.rojoma" %% "rojoma-json-v3" % "3.14.0",
      "io.spray" %% "spray-json" % "1.3.6",
      "org.parboiled" %% "parboiled" % "2.1.4",
      "com.fasterxml.jackson.core" % "jackson-annotations" % "2.9.10",
      "com.fasterxml.jackson.core" % "jackson-core" % "2.9.10",
      "com.fasterxml.jackson.core" % "jackson-databind" % "2.9.6",
      "com.google.code.gson" % "gson" % "2.8.9"
    )
  else Nil
}

// enable forking in run
run / fork := true
