name := "jawn-benchmarks"

javaOptions in run += "-Xmx6G"

libraryDependencies ++= Seq(
  "io.argonaut" %% "argonaut" % "6.1-M6",
  "org.json4s" %% "json4s-native" % "3.2.11",
  "org.json4s" %% "json4s-jackson" % "3.2.10",
  "com.typesafe.play" %% "play-json" % "2.3.0",
  "com.rojoma" %% "rojoma-json" % "2.4.3",
  "com.rojoma" %% "rojoma-json-v3" % "3.3.0",
  "io.spray" %% "spray-json" % "1.3.2",
  "org.parboiled" %% "parboiled" % "2.1.0",
  "com.fasterxml.jackson.core" % "jackson-annotations" % "2.5.3",
  "com.fasterxml.jackson.core" % "jackson-core" % "2.5.3",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.5.3",
  "com.google.code.gson" % "gson" % "2.2.4"
)

// enable forking in run
fork in run := true
