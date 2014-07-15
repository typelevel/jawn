package jawn
package ast

import java.io.File
import java.nio.ByteBuffer
import scala.util.Try

object JParser {
  implicit val facade = JawnFacade

  def parseUnsafe(s: String): JValue =
    new StringParser(s).parse()

  def parseFromString(s: String): Try[JValue] =
    Try(new StringParser[JValue](s).parse)

  def parseFromFile(file: File): Try[JValue] =
    Try(ChannelParser.fromFile[JValue](file).parse)

  def parseFromPath(path: String): Try[JValue] =
    parseFromFile(new File(path))

  def parseFromByteBuffer(buf: ByteBuffer): Try[JValue] =
    Try(new ByteBufferParser[JValue](buf).parse)

  def async(mode: AsyncParser.Mode): AsyncParser[JValue] =
    AsyncParser(mode)
}
