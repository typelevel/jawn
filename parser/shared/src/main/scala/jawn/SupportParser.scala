package org.typelevel.jawn

import java.nio.ByteBuffer
import scala.util.Try

trait SupportParser[J] extends SupportParserPlatform[J] {
  implicit def facade: Facade[J]

  def parseUnsafe(s: String): J =
    new StringParser(s).parse()

  def parseFromString(s: String): Try[J] =
    Try(new StringParser[J](s).parse())

  def parseFromCharSequence(cs: CharSequence): Try[J] =
    Try(new CharSequenceParser[J](cs).parse())

  def parseFromByteBuffer(buf: ByteBuffer): Try[J] =
    Try(new ByteBufferParser[J](buf).parse())

  def parseFromByteArray(src: Array[Byte]): Try[J] =
    Try(new ByteArrayParser[J](src).parse())

  def async(mode: AsyncParser.Mode): AsyncParser[J] =
    AsyncParser[J](mode)
}
