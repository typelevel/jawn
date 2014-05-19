package jawn

import java.io.File
import java.nio.ByteBuffer

object GenericParser {

  type Result[A] = Either[Exception, A]

  def parseUnsafe[J](s: String)(implicit facade: Facade[J]): J = new StringParser(s).parse()

  def parseFromString[J](s: String)(implicit facade: Facade[J]): Result[J] = try {
    Right(new StringParser[J](s).parse)
  } catch {
    case (e: Exception) => Left(e)
  }

  def parseFromPath[J](file: File)(implicit facade: Facade[J]): Result[J] = try {
    Right(ChannelParser.fromFile[J](file).parse)
  } catch {
    case (e: Exception) => Left(e)
  }

  def parseFromFile[J](file: File)(implicit facade: Facade[J]): Result[J] = try {
    Right(ChannelParser.fromFile[J](file).parse)
  } catch {
    case (e: Exception) => Left(e)
  }

  def parseFromByteBuffer[J](buf: ByteBuffer)(implicit facade: Facade[J]): Result[J] = try {
    Right(new ByteBufferParser[J](buf).parse)
  } catch {
    case (e: Exception) => Left(e)
  }

  def parseManyFromString[J](str: String)(implicit facade: Facade[J]): Result[Seq[J]] = try {
    Right(new StringParser[J](str).parseMany)
  } catch {
    case (e: Exception) => Left(e)
  }

  def parseManyFromFile[J](file: File)(implicit facade: Facade[J]): Result[Seq[J]] = try {
    Right(ChannelParser.fromFile[J](file).parseMany)
  } catch {
    case (e: Exception) => Left(e)
  }

  def parseManyFromByteBuffer[J](buf: ByteBuffer)(implicit facade: Facade[J]): Result[Seq[J]] = try {
    Right(new ByteBufferParser[J](buf).parseMany)
  } catch {
    case (e: Exception) => Left(e)
  }
}

object JParser {
  type Result[A] = Either[Exception, A]

  implicit val facade = JawnFacade

  def parseUnsafe(s: String): JValue = new StringParser(s).parse()

  def parseFromString(s: String): Result[JValue] = try {
    Right(new StringParser[JValue](s).parse)
  } catch {
    case (e: Exception) => Left(e)
  }

  def parseFromFile(file: File): Result[JValue] = try {
    Right(ChannelParser.fromFile[JValue](file).parse)
  } catch {
    case (e: Exception) => Left(e)
  }

  def parseFromPath(path: String): Result[JValue] =
    parseFromFile(new File(path))

  def parseFromByteBuffer(buf: ByteBuffer): Result[JValue] = try {
    Right(new ByteBufferParser[JValue](buf).parse)
  } catch {
    case (e: Exception) => Left(e)
  }

  def parseManyFromString(str: String): Result[Seq[JValue]] = try {
    Right(new StringParser[JValue](str).parseMany)
  } catch {
    case (e: Exception) => Left(e)
  }

  def parseManyFromFile(file: File): Result[Seq[JValue]] = try {
    Right(ChannelParser.fromFile[JValue](file).parseMany)
  } catch {
    case (e: Exception) => Left(e)
  }

  def parseManyFromByteBuffer(buf: ByteBuffer): Result[Seq[JValue]] = try {
    Right(new ByteBufferParser[JValue](buf).parseMany)
  } catch {
    case (e: Exception) => Left(e)
  }
}
