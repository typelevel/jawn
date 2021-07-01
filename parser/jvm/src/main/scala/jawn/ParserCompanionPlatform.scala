package org.typelevel.jawn

import java.io.File
import java.nio.channels.ReadableByteChannel
import scala.util.Try

private[jawn] trait ParserCompanionPlatform {

  def parseFromPath[J](path: String)(implicit facade: Facade[J]): Try[J] =
    Try(ChannelParser.fromFile[J](new File(path)).parse())

  def parseFromFile[J](file: File)(implicit facade: Facade[J]): Try[J] =
    Try(ChannelParser.fromFile[J](file).parse())

  def parseFromChannel[J](ch: ReadableByteChannel)(implicit facade: Facade[J]): Try[J] =
    Try(ChannelParser.fromChannel[J](ch).parse())

}
