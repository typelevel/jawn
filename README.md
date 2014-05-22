## Jawn

"Jawn is for parsing jay-sawn."

### Origin

The term "jawn" comes from the Philadelphia area. It conveys about as
much information as "thing" does. I chose the name because I had moved
to Montreal so I was remembering Philly fondly. Also, there isn't a
better way to describe objects encoded in JSON than "things". Finally,
we get a catchy slogan.

Jawn was designed to parse JSON into an AST as quickly as possible.

### Overview

Jawn consists of two parts:

1. A fast, generic JSON parser
2. A small, somewhat anemic AST

Currently Jawn is competitive with the fastest Java JSON libraries
(GSON and Jackson) and in the author's benchmarks it often wins. It
seems to be faster than any other Scala parser that exists (as of May
2014).

Given the plethora of really nice JSON libraries for Scala, the
expectation is that you are here for (1) and not (2).

### Parsing

Jawn's parser is both fast and relatively featureful. Assuming you
want to get back an AST of type `J` and you have a `Facade[J]`
defined, you can use the following `parse` signatures:

```scala
Parser.parseUnsafe[J](String) → J
Parser.parseFromString[J](String) → Try[J]
Parser.parsefromPath[J](String) → Try[J]
Parser.parseFromFile[J](File) → Try[J]
Parser.parseFromByteBuffer[J](ByteBuffer) → Try[J]
```

Some systems use streams of JSON values separated by whitespace. Jawn
can support this operation using the `parseMany` family of methods:

```scala
Parser.parseManyFromString[J](String) → Try[Seq[J]]
Parser.parseManyFromFile[J](File) → Try[Seq[J]]
Parser.parseManyFromByteBuffer[J](ByteBuffer) → Try[Seq[J]]
```

Finally, Jawn supports asynchronous parsing, which allows users to
feed the parser with data as it is available. There are three
constructors:

* `AsyncParser.json` waits to return a single `J` value once parsing is done.
* `AsyncParser.unwrap` if the top-level element is an array, return values as they become available.
* `AsyncParser.stream` same semantics as parseMany.



Here's an example:

```scala
val p0 = AsyncParser.unwrap[J]
val bb0: ByteBuffer = ...
val bb1: ByteBuffer = ...
val (AsyncParse(errors, values), p1) = p0(More(bb0))
val (AsyncParse(errors, values), p2) = p1(More(bb1))
val (AsyncParse(errors, values), _) = p2(Done)
```

### Do-It-Yourself Parsing

Jawn supports building any JSON AST you need via type classes. You
benefit from Jawn's fast parser while still using your favorite Scala
JSON library.

To include Jawn's parser in your project, add the following
snippet to your `build.sbt` file:

```scala
resolvers += "bintray/non" at "http://dl.bintray.com/non/maven"

libraryDependencies += "jawn" %% "jawn-parser" % "0.4.0"
```

To support your AST of choice, you'll want to define a
`jawn.Facade[J]` instance, where the `J` type parameter represents the
base of your JSON AST. For example, here's a facade that supports
Spray:

```scala
import spray.json._
object Spray extends SimpleFacade[JsValue] {
  def jnull() = JsNull
  def jfalse() = JsFalse
  def jtrue() = JsTrue
  def jnum(s: String) = JsNumber(s)
  def jint(s: String) = JsNumber(s)
  def jstring(s: String) = JsString(s)
  def jarray(vs: List[JsValue]) = JsArray(vs)
  def jobject(vs: Map[String, JsValue]) = JsObject(vs)
}
```

Most ASTs will be easy to define using the `SimpleFacade` or
`MutableFacade` traits. However, if an ASTs object or array instances
do more than just wrap a Scala collection, it may still make sense to
extend `Facade` directly.

### Examples

Jawn can parse JSON from many different sources:

 * `parseFromString(data: String)`
 * `parseFromFile(file: File)`
 * `parseFromPath(path: String)`
 * `parseFromByteBuffer(bb: ByteBuffer)`

Parsing returns `Either[Exception, JValue]`.

### Async Parsing

Jawn also supports async parsing. It's a bit complicated to set up,
but it works like a charm, and can be really useful in non-blocking
contexts.

```scala
import jawn.AsyncParser
import AsyncParser.{More, Done}

var parser1 = AsyncParser.json[JValue]

val bb1: ByteBuffer = ... // some input
val result1 = parser1(More(bb1))
val (AsyncParse(errors1, values1), parser2) = result1

...

val bb2: ByteBuffer = ... // more input
val result2 = parser2(More(bb2))
val (AsyncParse(errors2, values2), parser3) = result2

...

val result3 = parse3(Done)
val (AsyncParse(errors3, values3), _) = result3
```

(This really needs to be cleaned up. I'm just documenting the current
state of this feature.)

If you instantiate the parser via `AsyncParser.unwrap[JValue]` then it
will unwrap an outer array (this works really well when an array
really represents a large stream of simple JSON events.)

### Dependencies

Jawn currently depends on Scala 2.10. If you build it using SBT things
should just work.

There are some benchmarks and tests which have their own dependencies.

### Profiling

There are some micro-benchmarks using Caliper, as well as some ad-hoc
benchmarks. From SBT you can run the benchmarks like so:

```
> benchmark/run
```

Any JSON files you put in `benchmark/src/main/resources` will be
included in the ad-hoc benchmark. There is a Python script I've been
using to generate random JSON data called `randjson.py` which is a bit
quirky but does seem to work.

(I also test with very large data sets (100-600M) but for obvious
reasons I don't distribute this JSON in the project.)

Libraries currently being tested in order of average speed on tests
I've seen:

 * jawn
 * gson
 * play
 * jackson
 * json4s-jackson
 * rojoma
 * argonaut
 * smart-json
 * json4s-native
 * spray
 * lift-json (broken)

Of course, your mileage may vary, and these results do vary somewhat
based on file size, file structure, etc. Also note that Jackson
actually powers many of these libraries (including play, which
sometimes comes out faster than the explicit jackson test for reasons
I don't understand.)

I have tried to understand the libraries well enough to write the most
optimal code for loading a file (given a path) and parsing it to a
simple JSON AST.  Pull requests to update versions and improve usage
are welcome.

### Disclaimers

Jawn only supports UTF-8. This might change in the future, but for now
that's the target case. If you need full-featured support for
character encodings I imagine something like Jackson or Gson will work
better.

The library is still very immature so I'm sure there are some
bugs. There aren't even any formal tests yet! (Test-driven
development? What?) No liability or warranty is implied or
granted. This project was initially intended as a proof-of-concept for
the underlying design.

### Copyright and License

All code is available to you under the MIT license, available at
http://opensource.org/licenses/mit-license.php.

Copyright Erik Osheim, 2012-2013.
