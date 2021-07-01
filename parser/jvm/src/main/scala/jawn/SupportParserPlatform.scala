package org.typelevel.jawn

import java.io.File
import java.nio.channels.ReadableByteChannel
import scala.util.Try

private[jawn] trait SupportParserPlatform[J] { self: SupportParser[J] =>

  def parseFromPath(path: String): Try[J] =
    Try(ChannelParser.fromFile[J](new File(path)).parse())

  def parseFromFile(file: File): Try[J] =
    Try(ChannelParser.fromFile[J](file).parse())

  def parseFromChannel(ch: ReadableByteChannel): Try[J] =
    Try(ChannelParser.fromChannel[J](ch).parse())

}
