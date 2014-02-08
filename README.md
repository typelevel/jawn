## Jawn

"Jawn is for reading jay-sawn."

### Origin

The term "jawn" comes from the Philadelphia area. It conveys about as
much information as "thing" does. I chose the name because I moved to
Montreal so I am remembering Philly fondly. Also, there isn't a better
way to describe objects encoded in JSON than "things". Finally, we get
a catchy slogan.

### Overview

Jawn is designed to try to parse JSON into one large DOM-esque object
as fast as possible. There is a minimum of boxing, and there aren't
any fancy operations to build your own objects for you [1]. It uses
algebraic data types, following the "sealed-trait + case class"
pattern of many other JSON libraries in Scala.

([1] Although you can use the new `Facade[J]` type class to construct
your own AST using Jawn's parser. The benchmark project contains an
exmaple of this using Argonaut.)

The big speed win has to due with using Scala's `@tailrec`
optimization as heavily as possible, along with various other hacks,
and some semblence of an attempt at good IO
buffering/performance. There isn't really too much code, and profiling
shows that I'm spending about 50% of my time running
`java.lang.Double.parseDouble()` so that's something [2].

([2] Actually, Jawn will defer parsing doubles once it has determined
that they are valid JSON numbers. This comment applied back when Jawn
was eagerly parsing numbers.)

The `jawn.JValue` objects returned have very few capabilities: you
must break them open to get at their delicious brains, preferably
using pattern matching.  There's no reason that they can't have a
useful API but I haven't written one yet.

Jawn also lacks many other nice features, like keeping track of
whitespace or maintaining key/value order for JSON objects. This would
not be a hard feature to add but it isn't necessary in the general
case and would slow things down a bit. There also isn't any kind of
SAX-like API.

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
