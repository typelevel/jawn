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
