name := "jawn-ast"

libraryDependencies ++= Seq(
  "org.spire-math" %% "spire" % "0.7.4"
)

seq(bintrayResolverSettings: _*)

seq(bintrayPublishSettings: _*)
