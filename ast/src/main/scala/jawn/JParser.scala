package jawn

import java.io.File
import java.nio.ByteBuffer

import scala.util.Try

object GenericParser {

  def parseUnsafe[J](s: String)(implicit facade: Facade[J]): J =
    new StringParser(s).parse()

  def parseFromString[J](s: String)(implicit facade: Facade[J]): Try[J] =
    Try(new StringParser[J](s).parse)

  def parseFromPath[J](file: File)(implicit facade: Facade[J]): Try[J] =
    Try(ChannelParser.fromFile[J](file).parse)

  def parseFromFile[J](file: File)(implicit facade: Facade[J]): Try[J] =
    Try(ChannelParser.fromFile[J](file).parse)

  def parseFromByteBuffer[J](buf: ByteBuffer)(implicit facade: Facade[J]): Try[J] =
    Try(new ByteBufferParser[J](buf).parse)

  def parseManyFromString[J](str: String)(implicit facade: Facade[J]): Try[Seq[J]] =
    Try(new StringParser[J](str).parseMany)

  def parseManyFromFile[J](file: File)(implicit facade: Facade[J]): Try[Seq[J]] =
    Try(ChannelParser.fromFile[J](file).parseMany)

  def parseManyFromByteBuffer[J](buf: ByteBuffer)(implicit facade: Facade[J]): Try[Seq[J]] =
    Try(new ByteBufferParser[J](buf).parseMany)
}

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

  def parseManyFromString(str: String): Try[Seq[JValue]] =
    Try(new StringParser[JValue](str).parseMany)

  def parseManyFromFile(file: File): Try[Seq[JValue]] =
    Try(ChannelParser.fromFile[JValue](file).parseMany)

  def parseManyFromByteBuffer(buf: ByteBuffer): Try[Seq[JValue]] =
    Try(new ByteBufferParser[JValue](buf).parseMany)
}
