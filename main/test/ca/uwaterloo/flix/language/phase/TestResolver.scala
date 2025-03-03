/*
 *  Copyright 2017 Magnus Madsen
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package ca.uwaterloo.flix.language.phase

import ca.uwaterloo.flix.TestUtils
import ca.uwaterloo.flix.language.errors.ResolutionError
import ca.uwaterloo.flix.util.Options
import org.scalatest.FunSuite

class TestResolver extends FunSuite with TestUtils {

  test("AmbiguousTag.01") {
    val input =
      s"""
         |enum A {
         |  case Foo
         |}
         |
         |enum B {
         |  case Foo
         |}
         |
         |def f(): A = Foo
       """.stripMargin
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.AmbiguousTag](result)
  }

  test("AmbiguousTag.02") {
    val input =
      s"""
         |enum A {
         |  case Foo(Int32)
         |}
         |
         |enum B {
         |  case Foo(Int32)
         |}
         |
         |def f(): A = Foo(42)
       """.stripMargin
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.AmbiguousTag](result)
  }

  test("InaccessibleDef.01") {
    val input =
      s"""
         |namespace A {
         |  def f(): Int32 = 42
         |}
         |
         |namespace B {
         |  def g(): Int32 = A.f()
         |}
       """.stripMargin
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.InaccessibleDef](result)
  }

  test("InaccessibleDef.02") {
    val input =
      s"""
         |namespace A {
         |  def f(): Int32 = A/B/C.g()
         |
         |  namespace B/C {
         |    def g(): Int32 = A.f()
         |  }
         |}
       """.stripMargin
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.InaccessibleDef](result)
  }

  test("InaccessibleEnum.01") {
    val input =
      s"""
         |namespace A {
         |  enum Color {
         |    case Blu,
         |    case Red
         |  }
         |}
         |
         |namespace B {
         |  def g(): A.Color = A/Color.Red
         |}
       """.stripMargin
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.InaccessibleEnum](result)
  }

  test("InaccessibleEnum.02") {
    val input =
      s"""
         |namespace A {
         |  def f(): A/B/C.Color = A/B/C/Color.Blu
         |
         |  namespace B/C {
         |    enum Color {
         |      case Blu,
         |      case Red
         |    }
         |  }
         |}
       """.stripMargin
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.InaccessibleEnum](result)
  }

  test("OpaqueEnum.01") {
    val input =
      s"""
         |namespace A {
         |  pub opaque enum Color {
         |    case Blu,
         |    case Red
         |  }
         |}
         |
         |namespace B {
         |  def g(): A.Color = A/Color.Red
         |}
       """.stripMargin
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.OpaqueEnum](result)
  }

  test("OpaqueEnum.02") {
    val input =
      s"""
         |namespace A {
         |  def f(): A/B/C.Color = A/B/C/Color.Blu
         |
         |  namespace B/C {
         |    pub opaque enum Color {
         |      case Blu,
         |      case Red
         |    }
         |  }
         |}
       """.stripMargin
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.OpaqueEnum](result)
  }

  test("InaccessibleType.01") {
    val input =
      s"""
         |namespace A {
         |  enum Color {
         |    case Blu,
         |    case Red
         |  }
         |}
         |
         |namespace B {
         |  def g(): A.Color = ???
         |}
       """.stripMargin
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.InaccessibleEnum](result)
  }

  test("InaccessibleType.02") {
    val input =
      s"""
         |namespace A {
         |  def f(): A/B/C.Color = ???
         |
         |  namespace B/C {
         |    enum Color {
         |      case Blu,
         |      case Red
         |    }
         |  }
         |}
       """.stripMargin
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.InaccessibleEnum](result)
  }

  test("InaccessibleTypeAlias.01") {
    val input =
      s"""
         |namespace A {
         |  type alias Color = Int32
         |}
         |
         |namespace B {
         |  def g(): A.Color = 123
         |}
       """.stripMargin
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.InaccessibleTypeAlias](result)
  }

  test("InaccessibleTypeAlias.02") {
    val input =
      s"""
         |namespace A {
         |  def f(): A/B/C.Color = 123
         |
         |  namespace B/C {
         |    type alias Color = Int32
         |  }
         |}
       """.stripMargin
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.InaccessibleTypeAlias](result)
  }

  test("InaccessibleClass.01") {
    val input =
      s"""
         |namespace A {
         |  class Show[a] {
         |    pub def show(x: a): String
         |  }
         |}
         |
         |namespace B {
         |  def g(x: a): Int32 with A.Show[a] = ???
         |}
       """.stripMargin
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.InaccessibleClass](result)
  }

  test("InaccessibleClass.02") {
    val input =
      s"""
         |namespace A {
         |  def f(x: a): Int32 with A/B/C.Show[a] = ???
         |
         |  namespace B/C {
         |    class Show[a] {
         |      pub def show(x: a): String
         |    }
         |  }
         |}
       """.stripMargin
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.InaccessibleClass](result)
  }

  test("InaccessibleClass.03") {
    val input =
      """
        |namespace N {
        |    class C[a]
        |}
        |
        |namespace O {
        |    instance N.C[Int32]
        |}
        |""".stripMargin
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.InaccessibleClass](result)
  }

  test("InaccessibleClass.04") {
    val input =
      """
        |namespace N {
        |    class C[a]
        |}
        |
        |namespace O {
        |    class D[a] with N.C[a]
        |}
        |""".stripMargin
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.InaccessibleClass](result)
  }

  test("SealedClass.01") {
    val input =
      """
        |namespace N {
        |    pub sealed class C[a]
        |}
        |
        |namespace O {
        |    instance N.C[Int32]
        |}
        |""".stripMargin
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.SealedClass](result)
  }

  test("SealedClass.02") {
    val input =
      """
        |namespace N {
        |    sealed class C[a]
        |
        |    namespace O {
        |        instance N.C[Int32]
        |    }
        |}
        |""".stripMargin
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.SealedClass](result)
  }

  test("SealedClass.03") {
    val input =
      """
        |namespace N {
        |    sealed class C[a]
        |
        |    namespace O {
        |        class D[a] with N.C[a]
        |    }
        |}
        |""".stripMargin
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.SealedClass](result)
  }

  test("CyclicTypeAliases.01") {
    val input =
      s"""
         |type alias Foo = Foo
         |
         |def f(): Foo = 123
         |
       """.stripMargin
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.CyclicTypeAliases](result)
  }

  test("CyclicTypeAliases.02") {
    val input =
      s"""
         |type alias Foo = Bar
         |type alias Bar = Foo
         |
         |def f(): Foo = 123
         |
       """.stripMargin
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.CyclicTypeAliases](result)
  }

  test("CyclicTypeAliases.03") {
    val input =
      s"""
         |type alias Foo = Bar
         |type alias Bar = Baz
         |type alias Baz = Foo
         |
         |def f(): Foo = 123
         |
       """.stripMargin
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.CyclicTypeAliases](result)
  }

  test("CyclicTypeAliases.04") {
    val input =
      s"""
         |enum Option[t] {
         |    case None,
         |    case Some(t)
         |}
         |
         |type alias Foo = Option[Foo]
         |
         |def f(): Foo = 123
         |
       """.stripMargin
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.CyclicTypeAliases](result)
  }

  test("CyclicTypeAliases.05") {
    val input =
      s"""
         |enum Option[t] {
         |    case None,
         |    case Some(t)
         |}
         |
         |type alias Foo = Option[Bar]
         |type alias Bar = Option[Foo]
         |
         |def f(): Foo = 123
         |
       """.stripMargin
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.CyclicTypeAliases](result)
  }

  test("UndefinedName.01") {
    val input = "def f(): Int32 = x"
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.UndefinedName](result)
  }

  test("UndefinedName.02") {
    val input =
      s"""
         |namespace A {
         |  def f(x: Int32, y: Int32): Int32 = x + y + z
         |}
       """.stripMargin
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.UndefinedName](result)
  }

  test("UndefinedName.03") {
    val input =
      s"""
         |def foo(): #{ R } = #{}
       """.stripMargin
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.UndefinedName](result)
  }

  test("UndefinedName.04") {
    val input =
      s"""
         |namespace A {
         |    class C[a] {
         |        pub def f(x: a): a
         |    }
         |}
         |
         |namespace B {
         |    use A.f
         |    def g(): Int32 = f(1)
         |}
         |""".stripMargin
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.UndefinedName](result)
  }

  test("UndefinedEffect.01") {
    val input =
      """
        |def f(): Unit = try () with E {
        |    def op() = resume()
        |}
        |""".stripMargin
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.UndefinedEffect](result)
  }

  test("UndefinedOp.01") {
    val input =
      """
        |def f(): Unit = do E.op()
        |""".stripMargin
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.UndefinedOp](result)
  }

  test("UndefinedOp.02") {
    val input =
      """
        |eff E
        |
        |def f(): Unit = do E.op()
        |""".stripMargin
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.UndefinedOp](result)
  }

  test("UndefinedOp.03") {
    val input =
      """
        |eff E
        |
        |def f(): Unit = try () with E {
        |    def op() = resume()
        |}
        |""".stripMargin
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.UndefinedOp](result)
  }

  test("UndefinedClass.01") {
    val input =
      """
        |instance C[Int32]
        |""".stripMargin
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.UndefinedClass](result)
  }

  test("UndefinedClass.02") {
    val input =
      """
        |def f(x: a): a with C[a] = x
        |""".stripMargin
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.UndefinedClass](result)
  }

  test("UndefinedClass.03") {
    val input =
      """
        |class K[a]
        |
        |def f(x: a): a with K[a], U[a] = x
        |""".stripMargin
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.UndefinedClass](result)
  }

  test("UndefinedClass.04") {
    val input =
      """
        |class K[a]
        |
        |instance K[a] with U[a]
        |""".stripMargin
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.UndefinedClass](result)
  }

  test("UndefinedJvmConstructor.01") {
    val input =
      raw"""
           |def foo(): Unit =
           |    import new java.io.File(): ##java.io.File \ IO as _;
           |    ()
       """.stripMargin
    val result = compile(input, Options.TestWithLibMin)
    expectError[ResolutionError.UndefinedJvmConstructor](result)
  }

  test("UndefinedJvmConstructor.02") {
    val input =
      raw"""
           |def foo(): Unit =
           |    import new java.io.File(Int32): ##java.io.File \ IO as _;
           |    ()
       """.stripMargin
    val result = compile(input, Options.TestWithLibMin)
    expectError[ResolutionError.UndefinedJvmConstructor](result)
  }

  test("UndefinedJvmConstructor.03") {
    val input =
      raw"""
           |def foo(): Unit =
           |    import new java.lang.String(Bool): ##java.lang.String \ IO as _;
           |    ()
       """.stripMargin
    val result = compile(input, Options.TestWithLibMin)
    expectError[ResolutionError.UndefinedJvmConstructor](result)
  }

  test("UndefinedJvmConstructor.04") {
    val input =
      raw"""
           |def foo(): Unit =
           |    import new java.lang.String(Bool, Char, String): ##java.lang.String \ IO as _;
           |    ()
       """.stripMargin
    val result = compile(input, Options.TestWithLibMin)
    expectError[ResolutionError.UndefinedJvmConstructor](result)
  }

  test("UndefinedJvmClass.01") {
    val input =
      raw"""
           |def foo(): Unit =
           |    import new foo.bar.Baz(): Unit \ IO as newObject;
           |    ()
       """.stripMargin
    val result = compile(input, Options.TestWithLibMin)
    expectError[ResolutionError.UndefinedJvmClass](result)
  }

  test("UndefinedJvmClass.02") {
    val input =
      raw"""
           |def foo(): Unit =
           |    import foo.bar.Baz.f(): Unit \ IO;
           |    ()
       """.stripMargin
    val result = compile(input, Options.TestWithLibMin)
    expectError[ResolutionError.UndefinedJvmClass](result)
  }

  test("UndefinedJvmClass.03") {
    val input =
      raw"""
           |def foo(): Unit =
           |    import static foo.bar.Baz.f(): Unit \ IO;
           |    ()
       """.stripMargin
    val result = compile(input, Options.TestWithLibMin)
    expectError[ResolutionError.UndefinedJvmClass](result)
  }

  test("UndefinedJvmClass.04") {
    val input =
      raw"""
           |def foo(): Unit =
           |    import get foo.bar.Baz.f: Unit \ IO as getF;
           |    ()
       """.stripMargin
    val result = compile(input, Options.TestWithLibMin)
    expectError[ResolutionError.UndefinedJvmClass](result)
  }

  test("UndefinedJvmClass.05") {
    val input =
      raw"""
           |def foo(): Unit =
           |    import set foo.bar.Baz.f: Unit \ IO as setF;
           |    ()
       """.stripMargin
    val result = compile(input, Options.TestWithLibMin)
    expectError[ResolutionError.UndefinedJvmClass](result)
  }

  test("UndefinedJvmClass.06") {
    val input =
      raw"""
           |def foo(): Unit =
           |    import static get foo.bar.Baz.f: Unit \ IO as getF;
           |    ()
       """.stripMargin
    val result = compile(input, Options.TestWithLibMin)
    expectError[ResolutionError.UndefinedJvmClass](result)
  }

  test("UndefinedJvmClass.07") {
    val input =
      raw"""
           |def foo(): Unit =
           |    import static set foo.bar.Baz.f: Unit \ IO as setF;
           |    ()
       """.stripMargin
    val result = compile(input, Options.TestWithLibMin)
    expectError[ResolutionError.UndefinedJvmClass](result)
  }

  test("UndefinedJvmMethod.01") {
    val input =
      raw"""
           |def foo(): Unit =
           |    import java.lang.String.getFoo(): ##java.lang.String \ IO;
           |    ()
       """.stripMargin
    val result = compile(input, Options.TestWithLibMin)
    expectError[ResolutionError.UndefinedJvmMethod](result)
  }

  test("UndefinedJvmMethod.02") {
    val input =
      raw"""
           |def foo(): Unit =
           |    import java.lang.String.charAt(): ##java.lang.String \ IO;
           |    ()
       """.stripMargin
    val result = compile(input, Options.TestWithLibMin)
    expectError[ResolutionError.UndefinedJvmMethod](result)
  }

  test("UndefinedJvmMethod.03") {
    val input =
      raw"""
           |def foo(): Unit =
           |    import java.lang.String.charAt(Int32, Int32): ##java.lang.String \ IO;
           |    ()
       """.stripMargin
    val result = compile(input, Options.TestWithLibMin)
    expectError[ResolutionError.UndefinedJvmMethod](result)
  }

  test("UndefinedJvmMethod.04") {
    val input =
      raw"""
           |def foo(): Unit =
           |    import java.lang.String.isEmpty(Bool): Bool \ IO;
           |    ()
       """.stripMargin
    val result = compile(input, Options.TestWithLibMin)
    expectError[ResolutionError.UndefinedJvmMethod](result)
  }

  test("UndefinedJvmMethod.05") {
    val input =
      raw"""
           |def foo(): Unit =
           |    import static java.lang.String.isEmpty(): Bool \ IO;
           |    ()
       """.stripMargin
    val result = compile(input, Options.TestWithLibMin)
    expectError[ResolutionError.UndefinedJvmMethod](result)
  }

  test("UndefinedJvmMethod.06") {
    val input =
      raw"""
           |def foo(): Unit =
           |    import java.lang.String.valueOf(Bool): ##java.lang.String \ IO;
           |    ()
       """.stripMargin
    val result = compile(input, Options.TestWithLibMin)
    expectError[ResolutionError.UndefinedJvmMethod](result)
  }

  test("MismatchingReturnType.01") {
    val input =
      raw"""
           |def foo(): Unit =
           |    import java.lang.String.hashCode(): Unit \ IO as _;
           |    ()
       """.stripMargin
    val result = compile(input, Options.TestWithLibMin)
    expectError[ResolutionError.MismatchingReturnType](result)
  }

  test("MismatchingReturnType.02") {
    val input =
      raw"""
           |def foo(): Unit =
           |    import java.lang.String.subSequence(Int32, Int32): ##java.util.Iterator \ IO as _;
           |    ()
       """.stripMargin
    val result = compile(input, Options.TestWithLibMin)
    expectError[ResolutionError.MismatchingReturnType](result)
  }

  test("MismatchingReturnType.03") {
    val input =
      raw"""
           |type alias AliasedReturnType = ##java.util.Iterator
           |def foo(): Unit =
           |    import java.lang.String.subSequence(Int32, Int32): AliasedReturnType \ IO as _;
           |    ()
       """.stripMargin
    val result = compile(input, Options.TestWithLibMin)
    expectError[ResolutionError.MismatchingReturnType](result)
  }

  test("UndefinedJvmField.01") {
    val input =
      raw"""
           |def foo(): Unit =
           |    import get java.lang.Character.foo: ##java.lang.Character \ IO as getFoo;
           |    ()
           |
       """.stripMargin
    val result = compile(input, Options.TestWithLibMin)
    expectError[ResolutionError.UndefinedJvmField](result)
  }

  test("UndefinedJvmField.02") {
    val input =
      raw"""
           |def foo(): Unit =
           |    import set java.lang.Character.foo: ##java.lang.Character \ IO as setFoo;
           |    ()
       """.stripMargin
    val result = compile(input, Options.TestWithLibMin)
    expectError[ResolutionError.UndefinedJvmField](result)
  }

  test("UndefinedJvmField.03") {
    val input =
      raw"""
           |def foo(): Unit =
           |    import static get java.lang.Character.foo: ##java.lang.Character \ IO as getFoo;
           |    ()
       """.stripMargin
    val result = compile(input, Options.TestWithLibMin)
    expectError[ResolutionError.UndefinedJvmField](result)
  }

  test("UndefinedJvmField.04") {
    val input =
      raw"""
           |def foo(): Unit =
           |    import static set java.lang.Character.foo: Unit \ IO as setFoo;
           |    ()
       """.stripMargin
    val result = compile(input, Options.TestWithLibMin)
    expectError[ResolutionError.UndefinedJvmField](result)
  }

  test("UndefinedTag.01") {
    val input =
      s"""
         |enum A {
         |  case Foo
         |}
         |
         |def f(): A = A.Qux
         |
       """.stripMargin
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.UndefinedTag](result)
  }

  test("UndefinedTag.02") {
    val input =
      s"""
         |namespace A {
         |  enum B {
         |    case Foo,
         |    case Bar
         |  }
         |
         |  def f(): B = B.Qux(1 + 2)
         |}
       """.stripMargin
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.UndefinedTag](result)
  }

  test("UndefinedTag.03") {
    val input =
      s"""
         |namespace A {
         |  enum B {
         |    case Foo,
         |    case Bar
         |  }
         |
         |  def f(b: B): Int32 = match b {
         |    case B.Qux => 42
         |  }
         |}
       """.stripMargin
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.UndefinedTag](result)
  }

  test("UndefinedType.01") {
    val input = "def x(): Foo = 42"
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.UndefinedType](result)
  }

  test("UndefinedType.02") {
    val input =
      s"""namespace A {
         |  def foo(bar: Baz, baz: Baz): Qux = bar
         |}
       """.stripMargin
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.UndefinedType](result)
  }

  test("UndefinedType.03") {
    val input =
      s"""
         |namespace A {
         |  def f(): Int32 = Foo.Bar
         |}
       """.stripMargin
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.UndefinedType](result)
  }

  test("UndefinedType.04") {
    val input =
      s"""
         |namespace A {
         |  def f(): Int32 = Foo/Bar.Qux(true)
         |}
       """.stripMargin
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.UndefinedType](result)
  }

  test("UndefinedType.05") {
    val input =
      """
        |def f(): Unit \ E = ???
        |""".stripMargin
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.UndefinedType](result)
  }

  test("UndefinedType.06") {
    val input =
      """
        |def f(x: a -> b \ E): Unit = ???
        |""".stripMargin
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.UndefinedType](result)
  }


  test("CyclicClassHierarchy.01") {
    val input = "class A[a] with A[a]"
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.CyclicClassHierarchy](result)
  }

  test("CyclicClassHierarchy.02") {
    val input =
      """
        |class A[a] with B[a]
        |class B[a] with A[a]
        |""".stripMargin
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.CyclicClassHierarchy](result)
  }

  test("CyclicClassHierarchy.03") {
    val input =
      """
        |class A[a] with B[a]
        |class B[a] with C[a]
        |class C[a] with A[a]
        |""".stripMargin
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.CyclicClassHierarchy](result)
  }

  test("CyclicClassHierarchy.04") {
    val input =
      """
        |class A[a] with A[a], B[a]
        |class B[a]
        |""".stripMargin
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.CyclicClassHierarchy](result)
  }

  test("CyclicClassHierarchy.05") {
    val input =
      """
        |class A[a] with B[a]
        |class B[a] with A[a], C[a]
        |class C[a]
        |""".stripMargin
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.CyclicClassHierarchy](result)
  }

  test("DuplicateDerivation.01") {
    val input =
      """
        |enum E with Eq, Eq
        |""".stripMargin
    val result = compile(input, Options.TestWithLibMin)
    expectError[ResolutionError.DuplicateDerivation](result)
  }

  test("DuplicateDerivation.02") {
    val input =
      """
        |enum E with ToString, Order, ToString
        |""".stripMargin
    val result = compile(input, Options.TestWithLibMin)
    expectError[ResolutionError.DuplicateDerivation](result)
  }

  test("UnderAppliedTypeAlias.01") {
    val input =
      """
        |type alias T[a] = a
        |type alias S = T
        |""".stripMargin
    val result = compile(input, Options.TestWithLibMin)
    expectError[ResolutionError.UnderAppliedTypeAlias](result)
  }

  test("UnderAppliedTypeAlias.02") {
    val input =
      """
        |type alias T[a, b] = (a, b)
        |type alias S = T[Int32]
        |""".stripMargin
    val result = compile(input, Options.TestWithLibMin)
    expectError[ResolutionError.UnderAppliedTypeAlias](result)
  }

  test("UnderAppliedTypeAlias.03") {
    val input =
      """
        |type alias T[a] = a
        |
        |def f(x: T): Int32 = ???
        |""".stripMargin
    val result = compile(input, Options.TestWithLibMin)
    expectError[ResolutionError.UnderAppliedTypeAlias](result)
  }

  test("UnderAppliedTypeAlias.04") {
    val input =
      """
        |type alias T[a] = a
        |enum E[f: Type -> Type]
        |
        |def f(x: E[T]): Int32 = ???
        |""".stripMargin
    val result = compile(input, Options.TestWithLibMin)
    expectError[ResolutionError.UnderAppliedTypeAlias](result)
  }

  test("IllegalDerivation.01") {
    val input =
      """
        |class C[a]
        |
        |enum E with C
        |""".stripMargin
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.IllegalDerivation](result)
  }

  test("IllegalType.01") {
    val input =
      """
        |def isThisThingNull(x: a): Bool =
        |    import static java.util.Objects.isNull(a): Bool & Pure;
        |    isNull(x)
        |""".stripMargin
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.IllegalType](result)
  }

  test("IllegalNonJavaType.01") {
    val input =
      """
        |def f(): Unit =
        |    new Int32 {}
        |""".stripMargin
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.IllegalNonJavaType](result)
  }

  test("IllegalNonJavaType.02") {
    val input =
      """
        |def f(): Unit =
        |    new String {}
        |""".stripMargin
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.IllegalNonJavaType](result)
  }

  test("IllegalNonJavaType.03") {
    val input =
      """
        |type alias T = Int32
        |
        |def f(): Unit =
        |    new T {}
        |""".stripMargin
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.IllegalNonJavaType](result)
  }

  test("ParentNamespaceNotVisible.01") {
    val input =
      """
        |namespace A {
        |    pub enum X
        |    namespace B {
        |        def foo(): X = ???
        |    }
        |}
        |""".stripMargin
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.UndefinedType](result)
  }

  test("ParentNamespaceNotVisible.02") {
    val input =
      """
        |namespace A {
        |    pub type alias X = Int32
        |    namespace B {
        |        def foo(): X = ???
        |    }
        |}
        |""".stripMargin
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.UndefinedType](result)
  }

  test("ParentNamespaceNotVisible.03") {
    val input =
      """
        |namespace A {
        |    pub class X[a]
        |    namespace B {
        |        enum Y
        |        instance X[Y]
        |    }
        |}
        |""".stripMargin
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.UndefinedClass](result)
  }

  test("ParentNamespaceNotVisible.04") {
    val input =
      """
        |namespace A {
        |    pub def x(): Int32 = ???
        |    namespace B {
        |        def foo(): Int32 = x()
        |    }
        |}
        |""".stripMargin
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.UndefinedName](result)
  }

  test("UseClearedInNamespace.01") {
    val input =
      """
        |use A.X
        |namespace A {
        |  enum X
        |}
        |namespace B {
        |  def foo(): X = ???
        |}
        |""".stripMargin
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.UndefinedType](result)
  }

  test("UseClearedInNamespace.02") {
    val input =
      """
        |namespace A {
        |  enum X
        |}
        |namespace B {
        |  use A.X
        |  namespace C {
        |     def foo(): X = ???
        |  }
        |}
        |""".stripMargin
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.UndefinedType](result)
  }

  test("ImportClearedInNamespace.01") {
    val input =
      """
        |import java.lang.StringBuffer
        |namespace A {
        |  def foo(): StringBuffer = ???
        |}
        |""".stripMargin
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.UndefinedType](result)
  }

  test("ImportClearedInNamespace.02") {
    val input =
      """
        |namespace A {
        |  import java.lang.StringBuffer
        |  namespace B {
        |     def foo(): StringBuffer = ???
        |  }
        |}
        |""".stripMargin
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.UndefinedType](result)
  }

  test("TestParYield.01") {
    val input =
      """
        |def f(): Int32 =
        |    par (_ <- let b = 5; b) yield b
        |""".stripMargin
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.UndefinedName](result)
  }

  test("UndefinedTypeVar.Def.01") {
    val input = "def f[a: Type](): b = 123"
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.UndefinedTypeVar](result)
  }

  test("UndefinedTypeVar.Def.02") {
    val input = "def f[a: Type](x: b): Int = 123"
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.UndefinedTypeVar](result)
  }

  test("UndefinedTypeVar.Def.03") {
    val input = "def f[a: Type, b: Type, c: Type](x: Option[d]): Int = 123"
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.UndefinedTypeVar](result)
  }

  test("UndefinedTypeVar.Instance.01") {
    val input = "instance C[a] with C[b]"
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.UndefinedTypeVar](result)
  }

  test("UndefinedTypeVar.Instance.02") {
    val input = "instance C[(a, b)] with D[c]"
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.UndefinedTypeVar](result)
  }

  test("UndefinedTypeVar.Instance.03") {
    val input = "instance C[(a, b)] with D[a], D[b], D[c]"
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.UndefinedTypeVar](result)
  }

  test("UndefinedTypeVar.Class.01") {
    val input =
      """
        |class A[a]
        |class B[a] with A[b]
        |""".stripMargin
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.UndefinedTypeVar](result)
  }

  test("UndefinedTypeVar.Class.02") {
    val input =
      """
        |class A[a]
        |class B[a]
        |class C[a] with A[a], B[b]
        |""".stripMargin
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.UndefinedTypeVar](result)
  }

  test("UndefinedTypeVar.Expression.01") {
    val input =
      """
        |def f(): Bool = typematch () {
        |    case _: a => true
        |    case _: _ => false
        |}
        |""".stripMargin
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.UndefinedTypeVar](result)
  }

  test("IllegalSignature.01") {
    // The type variable `a` does not appear in the signature of `f`
    val input =
      """
        |class C[a] {
        |    pub def f(): Bool
        |}
        |""".stripMargin
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.IllegalSignature](result)
  }

  test("IllegalSignature.02") {
    val input =
      """
        |class C[a] {
        |    pub def f(): a
        |
        |    pub def g(): Bool
        |}
        |""".stripMargin
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.IllegalSignature](result)
  }

  test("IllegalSignature.03") {
    val input =
      """
        |class C[a] {
        |    pub def f(x: {y = a}): {y = Bool}
        |
        |    pub def g(x: {y = Bool}): Bool
        |}
        |""".stripMargin
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.IllegalSignature](result)
  }

  test("IllegalSignature.04") {
    val input =
      """
        |class C[a] {
        |    pub def f(): a
        |
        |    pub def g(): Bool
        |
        |    pub def h(): a
        |}
        |""".stripMargin
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.IllegalSignature](result)
  }

  test("IllegalSignature.05") {
    val input =
      """
        |class C[a] {
        |    pub def f(): Int
        |
        |    pub def g(): String
        |
        |    pub def h(): a
        |}
        |""".stripMargin
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.IllegalSignature](result)
  }

  test("IllegalWildType.01") {
    val input =
      """
        |type alias T = _
        |""".stripMargin
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.IllegalWildType](result)
  }

  test("IllegalWildType.02") {
    val input =
      """
        |type alias T = _ -> _
        |""".stripMargin
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.IllegalWildType](result)
  }

  test("IllegalWildType.03") {
    val input =
      """
        |enum E {
        |    case C(_)
        |}
        |""".stripMargin
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.IllegalWildType](result)
  }

  test("IllegalWildType.04") {
    val input =
      """
        |def foo(): String = unsafe_cast 123 as _
        |""".stripMargin
    val result = compile(input, Options.TestWithLibNix)
    expectError[ResolutionError.IllegalWildType](result)
  }

  test("IllegalWildType.05") {
    val input =
      """
        |def foo(): String \ IO = {
        |    import java.util.Arrays.deepToString(Array[_, _], Int32): String \ IO;
        |    deepToString([])
        |}
        |""".stripMargin
    val result = compile(input, Options.TestWithLibMin)
    expectError[ResolutionError.IllegalWildType](result)
  }

  test("IllegalWildType.06") {
    val input =
      """
        |def foo(): String \ IO = {
        |    import java.util.Arrays.deepToString(Array[Int32, Static], Int32): _ \ IO;
        |    deepToString([])
        |}
        |""".stripMargin
    val result = compile(input, Options.TestWithLibMin)
    expectError[ResolutionError.IllegalWildType](result)
  }
}
