package org.typelevel.jawn
package ast

import java.io.File
import java.nio.channels.ReadableByteChannel
import scala.util.Try

private[jawn] trait JParserPlatform { self: JParser.type =>

  def parseFromPath(path: String): Try[JValue] =
    parseFromFile(new File(path))

  def parseFromFile(file: File): Try[JValue] =
    Try(ChannelParser.fromFile[JValue](file).parse())

  def parseFromChannel(ch: ReadableByteChannel): Try[JValue] =
    Try(ChannelParser.fromChannel(ch).parse())

}
