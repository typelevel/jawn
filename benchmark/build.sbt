name := "jawn-benchmarks"

javaOptions in run += "-Xmx6G"

resolvers ++= Seq(
  "mth.io snapshots" at "http://repo.mth.io/snapshots",
  "mth.io releases" at "http://repo.mth.io/releases",
  "snapshots" at "http://oss.sonatype.org/content/repositories/snapshots",
  "releases"  at "http://oss.sonatype.org/content/repositories/releases",
  "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"
)

libraryDependencies ++= Seq(
  "io.spray"          %% "spray-json"     % "1.2.5",
  "io.spray"          %% "spray-json"     % "1.2.5",
  "com.typesafe.play" %% "play-json"      % "2.2.1",
  "org.json4s"        %% "json4s-jackson" % "3.2.6",
  "org.json4s"        %% "json4s-native"  % "3.2.6",
  "org.parboiled"     %% "parboiled"      % "2.0-M1",
  "com.rojoma"        %% "rojoma-json" % "2.4.0",
  "io.argonaut"       %% "argonaut" % "6.1-M2",
  "org.scalastuff" %% "json-parser" % "1.1.1",
  //"net.liftweb"        % "lift-json_2.9.2" % "2.5-M1",
  "net.minidev"        % "json-smart" % "1.1.1",
  "com.fasterxml.jackson.core" % "jackson-annotations" % "2.0.6",
  "com.fasterxml.jackson.core" % "jackson-core" % "2.0.6",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.0.6",
  "com.google.guava" % "guava" % "r09",
  "com.google.code.java-allocation-instrumenter" % "java-allocation-instrumenter" % "2.0",
  "com.google.code.caliper" % "caliper" % "1.0-SNAPSHOT" from "http://plastic-idolatry.com/jars/caliper-1.0-SNAPSHOT.jar",
  "com.google.code.gson" % "gson" % "2.2.4"
)

// enable forking in run
fork in run := true
