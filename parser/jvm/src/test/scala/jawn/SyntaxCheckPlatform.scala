/*
 * Copyright (c) 2022 Typelevel
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.typelevel.jawn
package parser

import org.scalacheck.Prop
import Facade.NullFacade
import scala.util.Try
import scala.util.Failure

private[jawn] trait SyntaxCheckPlatform { self: SyntaxCheck =>

  def isValidSyntaxPlatform(s: String): Boolean =
    TestUtil.withTemp(s)(t => Parser.parseFromFile(t)(NullFacade).isSuccess)

  def testErrorLocPlatform(json: String, line: Int, col: Int): Prop = {
    import java.io.ByteArrayInputStream
    import java.nio.channels.{Channels, ReadableByteChannel}

    def ch(s: String): ReadableByteChannel =
      Channels.newChannel(new ByteArrayInputStream(s.getBytes("UTF-8")))

    def assertLoc(p: ParseException): Prop =
      Prop(p.line == line && p.col == col)

    def fail(msg: String): Prop =
      Prop.falsified :| msg

    def extract1(t: Try[Unit]): Prop =
      t match {
        case Failure(p @ ParseException(_, _, _, _)) => assertLoc(p)
        case otherwise => fail(s"expected Failure(ParseException), got $otherwise")
      }

    extract1(Parser.parseFromChannel(ch(json))(NullFacade))
  }

}
