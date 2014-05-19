package jawn

import scala.annotation.{switch, tailrec}
import scala.collection.mutable

/**
 * SyncParser extends Parser to do all parsing synchronously.
 * 
 * Most traditional JSON parser are synchronous, and expect to receive
 * all their input before returning. SyncParser[J] still leaves
 * Parser[J]'s methods abstract, but adds two public methods for users
 * to call to actually parse JSON.
 */
trait SyncParser[J] extends Parser[J] {

  /**
   * Parse the JSON document into a single JSON value.
   *
   * The parser considers documents like '333', 'true', and '"foo"' to be
   * valid, as well as more traditional documents like [1,2,3,4,5]. However,
   * multiple top-level objects are not allowed.
   */
  final def parse()(implicit facade: Facade[J]): J = {
    val (value, i) = parse(0)
    var j = i
    while (!atEof(j)) {
      (at(j): @switch) match {
        case '\n' => newline(j); j += 1
        case ' ' | '\t' | '\r' => j += 1
        case _ => die(j, "expected whitespace or eof")
      }
    }
    if (!atEof(j)) die(j, "expected eof")
    close()
    value
  }

  /**
   * Parse the given document into a sequence of JSON values. These
   * might be containers like objects and arrays, or primtitives like
   * numbers and strings.
   *
   * JSON objects may only be separated by whitespace. Thus,
   * "top-level" commas and other characters will become parse errors.
   */
  final def parseMany()(implicit facade: Facade[J]): Seq[J] = {
    val results = mutable.ArrayBuffer.empty[J]
    var i = 0
    while (!atEof(i)) {
      (at(i): @switch) match {
        case '\n' => newline(i); i += 1
        case ' ' | '\t' | '\r' => i += 1 
        case _ =>
          val (value, j) = parse(i)
          results.append(value)
          i = j
      }
    }
    close()
    results
  }
}
