package org.typelevel.jawn
package ast

import java.nio.ByteBuffer
import scala.util.Try

object JParser extends JParserPlatform {
  implicit val facade: Facade[JValue] = JawnFacade

  def parseUnsafe(s: String): JValue =
    new StringParser(s).parse()

  def parseFromString(s: String): Try[JValue] =
    Try(new StringParser[JValue](s).parse())

  def parseFromCharSequence(cs: CharSequence): Try[JValue] =
    Try(new CharSequenceParser[JValue](cs).parse())

  def parseFromByteBuffer(buf: ByteBuffer): Try[JValue] =
    Try(new ByteBufferParser[JValue](buf).parse())

  def async(mode: AsyncParser.Mode): AsyncParser[JValue] =
    AsyncParser(mode)
}
