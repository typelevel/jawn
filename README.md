## Jawn

"Jawn is for reading jay-sawn."

### Origin

The term "jawn" comes from the Philadelphia area. It conveys about as much
information as "thing" does. I chose the name because I moved to Montreal so I
am remembering Philly fondly. Also, there isn't a better way to describe
objects encoded in JSON than "things". Finally, we get a catchy slogan.

### Overview

Jawn is designed to try to parse JSON into one large DOM-esque object as fast
as possible. There is a minimum of boxing, and there aren't any fancy
operations to build your own objects for you. It uses algebraic data types,
following the "sealed-trait + case class" pattern of many other JSON libraries
in Scala.

The big speed win has to due with using Scala's `@tailrec` optimization as
heavily as possible, along with various other hacks, and some semblence of an
attempt at good IO buffering/performance. There isn't really too much code,
and profiling shows that I'm spending about 50% of my time running
`java.lang.Double.parseDouble()` so that's something.

Jawn is currently single-threaded: the design probably permits multi-threaded
loading but that's for the future.

The `jawn.Value` objects returned have very few capabilities: you must break
them open to get at their delicious brains, preferably using pattern matching.
There's no reason that they can't have a useful API but I haven't written one
yet.

Jawn also lacks many other nice features, like keeping track of whitespace or
maintaining key/value order for JSON objects. This would not be a hard feature
to add but it isn't necessary in the general case and would slow things down a
bit. There also isn't any kind of SAX-like API.

### Examples

Jawn can parse a string:

```scala
val s = "[1, 2.0, \"three\", null]"
val o = jawn.Parser.parseString(s)
```

...or a path:

```scala
val o = jawn.Parser.parsePath("data.json")
```

Jawn objects can also generate a valid JSON representation of themselves using
the `j` method (although as mentioned above, it is not guaranteed to be the
same JSON representation they were read with):

```scala
val s = "[1, 2.0, \"three\", null]"
val o = jawn.Parser.parseString(s)
o.j // -> "[1, 2.0, \"three\", null]"
```

### Dependencies

Jawn currently depends on Scala 2.10.0-M7, as well as an (unreleased) version
of the Debox library. If you build it using SBT things should just work,
otherwise you may need to fiddle with things a bit.

There are some benchmarks and tests which have their own dependencies.

### Profiling

There are some micro-benchmarks using Caliper, as well as some ad-hoc
benchmarks. From SBT you can run the benchmarks like so:

```
> project benchmark
> run
```

Any JSON files you put in `benchmark/src/main/resources` will be included in
the ad-hoc benchmark. There is a Python script I've been using to generate
random JSON data called `randjson.py` which is a bit quirky but does seem to
work.

I have comparisons against smart-json and Jackson right now, and will probably
add more. Right now Jawn seems to build trees faster (which makes sense since
it lacks many general purpose capabilities). I'm happy to get pull requests
either adding new Java/Scala libraries to test against, or using more effient
methods to build the tree.

### Disclaimers

Jawn only supports UTF-8. This might change in the future, but for now that's
the target case. If you need full-featured support for character encodings I
imagine something like Jackson will work better.

The library is still very immature so I'm sure there are some bugs. There
aren't even any formal tests yet! (Test-driven development? What?) No
liability or warranty is implied or granted. This project was initially
intended as a proof-of-concept for the underlying design.

### Copyright and License

All code is available to you under the MIT license, available at
http://opensource.org/licenses/mit-license.php.

Copyright Erik Osheim, 2012.
