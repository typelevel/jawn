/*
 * Copyright (c) 2012 Typelevel
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
package ast

import scala.annotation.{nowarn, switch}
import scala.collection.mutable
import scala.util.Sorting

object Render {
  sealed abstract private[ast] class Frame
  final private[ast] class ArrFrame(val vs: Array[JValue]) extends Frame { var i: Int = 0 }
  final private[ast] class ObjFrame(val it: Iterator[(String, JValue)]) extends Frame { var started: Boolean = false }
}

sealed trait Renderer {
  import Render.{ArrFrame, Frame, ObjFrame}

  final def render(jv: JValue): String = {
    val sb = new StringBuilder
    render(sb, jv)
    sb.toString
  }

  final def render(sb: StringBuilder, jv: JValue): Unit = {
    val stack = mutable.ArrayBuffer.empty[Frame]
    open(sb, jv, stack)
    drain(sb, stack)
  }

  @nowarn("msg=used")
  @deprecated("Preserved for binary compatibility. Use the overload without the depth parameter.", "1.8.0")
  final def render(sb: StringBuilder, depth: Int, jv: JValue): Unit =
    render(sb, jv)

  def canonicalizeObject(vs: mutable.Map[String, JValue]): Iterator[(String, JValue)]

  def renderString(sb: StringBuilder, s: String): Unit

  @nowarn("msg=used")
  @deprecated("Preserved for binary compatibility. Use render(sb, JArray(vs)).", "1.8.0")
  final def renderArray(sb: StringBuilder, depth: Int, vs: Array[JValue]): Unit =
    render(sb, JArray(vs))

  final def renderObject(sb: StringBuilder, it: Iterator[(String, JValue)]): Unit =
    if (!it.hasNext) {
      sb.append("{}")
      ()
    } else {
      sb.append('{')
      val stack = mutable.ArrayBuffer.empty[Frame]
      stack += new ObjFrame(it)
      drain(sb, stack)
    }

  @nowarn("msg=used")
  @deprecated("Preserved for binary compatibility. Use the overload without the depth parameter.", "1.8.0")
  final def renderObject(sb: StringBuilder, depth: Int, it: Iterator[(String, JValue)]): Unit =
    renderObject(sb, it)

  private def open(sb: StringBuilder, jv: JValue, stack: mutable.ArrayBuffer[Frame]): Unit =
    jv match {
      case JNull => sb.append("null")
      case JTrue => sb.append("true")
      case JFalse => sb.append("false")
      case LongNum(n) => sb.append(n.toString)
      case DoubleNum(n) => sb.append(n.toString)
      case DeferNum(s) => sb.append(s)
      case DeferLong(s) => sb.append(s)
      case JString(s) => renderString(sb, s)
      case JArray(vs) =>
        if (vs.isEmpty) sb.append("[]")
        else {
          sb.append('[')
          stack += new ArrFrame(vs)
        }
      case JObject(vs) =>
        val it = canonicalizeObject(vs)
        if (!it.hasNext) sb.append("{}")
        else {
          sb.append('{')
          stack += new ObjFrame(it)
        }
    }

  private def drain(sb: StringBuilder, stack: mutable.ArrayBuffer[Frame]): Unit =
    while (stack.nonEmpty)
      stack.last match {
        case f: ArrFrame =>
          if (f.i < f.vs.length) {
            if (f.i > 0) sb.append(',')
            val v = f.vs(f.i)
            f.i += 1
            open(sb, v, stack)
          } else {
            sb.append(']')
            stack.remove(stack.length - 1)
          }
        case f: ObjFrame =>
          if (f.it.hasNext) {
            if (f.started) sb.append(',')
            else f.started = true

            val (k, v) = f.it.next()
            renderString(sb, k)
            sb.append(':')
            open(sb, v, stack)
          } else {
            sb.append('}')
            stack.remove(stack.length - 1)
          }
      }

  final def escape(sb: StringBuilder, s: String, unicode: Boolean): Unit = {
    sb.append('"')
    var i = 0
    val len = s.length
    while (i < len) {
      (s.charAt(i): @switch) match {
        case '"' => sb.append("\\\"")
        case '\\' => sb.append("\\\\")
        case '\b' => sb.append("\\b")
        case '\f' => sb.append("\\f")
        case '\n' => sb.append("\\n")
        case '\r' => sb.append("\\r")
        case '\t' => sb.append("\\t")
        case c =>
          if (c < ' ' || (c > '~' && unicode)) sb.append("\\u%04x".format(c.toInt))
          else sb.append(c)
      }
      i += 1
    }
    sb.append('"')
  }
}

object CanonicalRenderer extends Renderer {
  def canonicalizeObject(vs: mutable.Map[String, JValue]): Iterator[(String, JValue)] = {
    val keys = vs.keys.toArray
    Sorting.quickSort(keys)
    keys.iterator.map(k => (k, vs(k)))
  }
  def renderString(sb: StringBuilder, s: String): Unit =
    escape(sb, s, true)
}

object FastRenderer extends Renderer {
  def canonicalizeObject(vs: mutable.Map[String, JValue]): Iterator[(String, JValue)] =
    vs.iterator
  def renderString(sb: StringBuilder, s: String): Unit =
    escape(sb, s, false)
}
