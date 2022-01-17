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
package util

import org.scalacheck.Prop
import scala.util.Try

import Prop.forAll

private[jawn] trait SliceCheckPlatform { self: SliceCheck =>

  // substring has very different semantics on JS
  // https://stackoverflow.com/questions/54313862/scala-substring-correctly-throws-indexoutofbounds-but-does-not-in-scala-js

  property("slice.charAt(i) ~ slice.toString.charAt(i)") = forAll { (cs: Slice, i: Int) =>
    tryEqual(cs.charAt(i), cs.toString.charAt(i))
  }

  property("Slice(s, i, j) ~ s.substring(i, j)") = forAll { (s: String, i: Int, j: Int) =>
    tryEqual(Slice(s, i, j).toString, s.substring(i, j))
  }

  property("Slice(s, i, j).charAt(k) ~ s.substring(i, j).charAt(k)") = forAll { (s: String, i: Int, j: Int, k: Int) =>
    tryEqual(Slice(s, i, j).charAt(k), s.substring(i, j).charAt(k))
  }

  property("Slice(s, i, j).subSequence(k, l) ~ s.substring(i, j).substring(k, l)") = forAll {
    (s: String, i: Int, j: Int, k: Int, l: Int) =>
      tryEqual(Slice(s, i, j).subSequence(k, l).toString, s.substring(i, j).substring(k, l))
  }

  property("slice is serializable") = {
    import java.io._

    forAll { (x: Slice) =>
      val baos = new ByteArrayOutputStream()
      val oos = new ObjectOutputStream(baos)
      oos.writeObject(x)
      oos.close()
      val bytes = baos.toByteArray
      val bais = new ByteArrayInputStream(bytes)
      val ois = new ObjectInputStream(bais)
      val res = Prop(Try(ois.readObject()) == Try(x))
      ois.close()
      res
    }
  }

}
