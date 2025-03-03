/*
 * Copyright 2015-2016 Magnus Madsen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ca.uwaterloo.flix.language.ast

import ca.uwaterloo.flix.api.Flix
import ca.uwaterloo.flix.language.fmt.{Audience, FormatOptions, FormatType}
import ca.uwaterloo.flix.util.InternalCompilerException

import java.util.Objects
import scala.annotation.tailrec
import scala.collection.immutable.SortedSet

/**
  * Representation of types.
  */
sealed trait Type {

  /**
    * The kind of `this` type.
    */
  def kind: Kind

  /**
    * The location of `this` type.
    */
  def loc: SourceLocation

  /**
    * Returns the type variables in `this` type.
    *
    * Returns a sorted set to ensure that the compiler is deterministic.
    */
  def typeVars: SortedSet[Type.Var] = this match {
    case x: Type.Var => SortedSet(x)
    case Type.Cst(tc, _) => SortedSet.empty
    case Type.Apply(tpe1, tpe2, _) => tpe1.typeVars ++ tpe2.typeVars
    case Type.Alias(_, args, _, _) => args.flatMap(_.typeVars).to(SortedSet)
  }

  /**
    * Gets all the effects in the given type.
    */
  def effects: SortedSet[Symbol.EffectSym] = this match {
    case Type.Cst(TypeConstructor.Effect(sym), _) => SortedSet(sym)

    case _: Type.Cst => SortedSet.empty
    case _: Type.Var => SortedSet.empty

    case Type.Apply(tpe1, tpe2, _) => tpe1.effects ++ tpe2.effects
    case Type.Alias(_, _, tpe, _) => tpe.effects
  }

  /**
    * Optionally returns the type constructor of `this` type.
    *
    * Return `None` if the type constructor is a variable.
    *
    * Otherwise returns `Some(tc)` where `tc` is the left-most type constructor.
    *
    * For example,
    *
    * {{{
    * x                             =>      None
    * Celsius                       =>      Some(Celsius)
    * Option[Int]                   =>      Some(Option)
    * Arrow[Bool, Char]             =>      Some(Arrow)
    * Tuple[Bool, Int]              =>      Some(Tuple)
    * Result[Bool, Int]             =>      Some(Result)
    * Result[Bool][Int]             =>      Some(Result)
    * Option[Result[Bool, Int]]     =>      Some(Option)
    * }}}
    */
  def typeConstructor: Option[TypeConstructor] = this match {
    case Type.Var(_, _) => None
    case Type.Cst(tc, _) => Some(tc)
    case Type.Apply(t1, _, _) => t1.typeConstructor
    case Type.Alias(_, _, tpe, _) => tpe.typeConstructor
  }

  /**
    * Returns the base type of a type application.
    * Similar to [[Type.typeConstructor]] except that here we return the type (rather than the constructor),
    * which may be a non-[[Type.Cst]] type.
    *
    * {{{
    * Option[Int] => Option
    * a[Int]      => a
    * }}}
    *
    */
  def baseType: Type.BaseType = this match {
    case Type.Apply(t1, _, _) => t1.baseType
    case bt: Type.BaseType => bt
  }

  /**
    * Returns a list of all type constructors in `this` type.
    */
  def typeConstructors: List[TypeConstructor] = this match {
    case Type.Var(_, _) => Nil
    case Type.Cst(tc, _) => tc :: Nil
    case Type.Apply(t1, t2, _) => t1.typeConstructors ::: t2.typeConstructors
    case Type.Alias(_, _, tpe, _) => tpe.typeConstructors
  }

  /**
    * Returns the type arguments of `this` type.
    *
    * For example,
    *
    * {{{
    * Celsius                       =>      Nil
    * Option[Int]                   =>      Int :: Nil
    * Arrow[Bool, Char]             =>      Bool :: Char :: Nil
    * Tuple[Bool, Int]              =>      Bool :: Int :: Nil
    * Result[Bool, Int]             =>      Bool :: Int :: Nil
    * Result[Bool][Int]             =>      Bool :: Int :: Nil
    * Option[Result[Bool, Int]]     =>      Result[Bool, Int] :: Nil
    * }}}
    */
  def typeArguments: List[Type] = this match {
    case Type.Apply(tpe1, tpe2, _) => tpe1.typeArguments ::: tpe2 :: Nil
    case _ => Nil
  }

  /**
    * Applies `f` to every type variable in `this` type.
    */
  def map(f: Type.Var => Type): Type = this match {
    case tvar: Type.Var => f(tvar)
    case Type.Cst(_, _) => this
    case Type.Apply(tpe1, tpe2, loc) => Type.Apply(tpe1.map(f), tpe2.map(f), loc)
    case Type.Alias(sym, args, tpe, loc) => Type.Alias(sym, args.map(_.map(f)), tpe.map(f), loc)
  }

  /**
    * Returns the argument types of `this` arrow type.
    *
    * NB: Assumes that `this` type is an arrow.
    */
  def arrowArgTypes: List[Type] = typeConstructor match {
    case Some(TypeConstructor.Arrow(n)) => typeArguments.drop(2).dropRight(1)
    case _ => throw InternalCompilerException(s"Unexpected non-arrow type: '$this'.", loc)
  }

  /**
    * Returns the result type of `this` arrow type.
    *
    * NB: Assumes that `this` type is an arrow.
    */
  def arrowResultType: Type = typeConstructor match {
    case Some(TypeConstructor.Arrow(n)) => typeArguments.last
    case _ => throw InternalCompilerException(s"Unexpected non-arrow type: '$this'.", loc)
  }

  /**
    * Returns the purity type of `this` arrow type.
    *
    * NB: Assumes that `this` type is an arrow.
    */
  def arrowPurityType: Type = typeConstructor match {
    case Some(TypeConstructor.Arrow(n)) => typeArguments.head
    case _ => throw InternalCompilerException(s"Unexpected non-arrow type: '$this'.", loc)
  }

  /**
    * Returns the effect type of `this` arrow type.
    *
    * NB: Assumes that `this` type is an arrow.
    */
  def arrowEffectType: Type = typeConstructor match {
    case Some(TypeConstructor.Arrow(n)) => typeArguments(1)
    case _ => throw InternalCompilerException(s"Unexpected non-arrow type: '$this'.", loc)
  }

  /**
    * Returns the size of `this` type.
    */
  def size: Int = this match {
    case Type.Var(_, _) => 1
    case Type.Cst(_, _) => 1
    case Type.Apply(tpe1, tpe2, _) => tpe1.size + tpe2.size + 1
    case Type.Alias(_, _, tpe, _) => tpe.size
  }

  /**
    * Returns a human readable string representation of `this` type.
    */
  override def toString: String = FormatType.formatTypeWithOptions(this, FormatOptions.Internal)
}

object Type {

  /////////////////////////////////////////////////////////////////////////////
  // Constants                                                               //
  /////////////////////////////////////////////////////////////////////////////

  // TODO: Reduce usage of these in favor of the ones with source locations.

  /**
    * Represents the Unit type.
    */
  val Unit: Type = Type.Cst(TypeConstructor.Unit, SourceLocation.Unknown)

  /**
    * Represents the Null type.
    */
  val Null: Type = Type.Cst(TypeConstructor.Null, SourceLocation.Unknown)

  /**
    * Represents the Bool type.
    */
  val Bool: Type = Type.Cst(TypeConstructor.Bool, SourceLocation.Unknown)

  /**
    * Represents the Char type.
    */
  val Char: Type = Type.Cst(TypeConstructor.Char, SourceLocation.Unknown)

  /**
    * Represents the Float32 type.
    */
  val Float32: Type = Type.Cst(TypeConstructor.Float32, SourceLocation.Unknown)

  /**
    * Represents the Float64 type.
    */
  val Float64: Type = Type.Cst(TypeConstructor.Float64, SourceLocation.Unknown)

  /**
    * Represents the BigDecimal type.
    */
  val BigDecimal: Type = Type.Cst(TypeConstructor.BigDecimal, SourceLocation.Unknown)

  /**
    * Represents the Int8 type.
    */
  val Int8: Type = Type.Cst(TypeConstructor.Int8, SourceLocation.Unknown)

  /**
    * Represents the Int16 type.
    */
  val Int16: Type = Type.Cst(TypeConstructor.Int16, SourceLocation.Unknown)

  /**
    * Represents the Int32 type.
    */
  val Int32: Type = Type.Cst(TypeConstructor.Int32, SourceLocation.Unknown)

  /**
    * Represents the Int64 type.
    */
  val Int64: Type = Type.Cst(TypeConstructor.Int64, SourceLocation.Unknown)

  /**
    * Represents the BigInt type.
    */
  val BigInt: Type = Type.Cst(TypeConstructor.BigInt, SourceLocation.Unknown)

  /**
    * Represents the String type.
    */
  val Str: Type = Type.Cst(TypeConstructor.Str, SourceLocation.Unknown)

  /**
    * Represents the Lazy type constructor.
    *
    * NB: This type has kind: * -> *.
    */
  val Lazy: Type = Type.Cst(TypeConstructor.Lazy, SourceLocation.Unknown)

  /**
    * Represents the Relation type constructor.
    */
  val Relation: Type = Type.Cst(TypeConstructor.Relation, SourceLocation.Unknown)

  /**
    * Represents the Lattice type constructor.
    */
  val Lattice: Type = Type.Cst(TypeConstructor.Lattice, SourceLocation.Unknown)

  /**
    * Represents the type of an empty record.
    */
  val RecordRowEmpty: Type = Type.Cst(TypeConstructor.RecordRowEmpty, SourceLocation.Unknown)

  /**
    * Represents the type of an empty schema.
    */
  val SchemaRowEmpty: Type = Type.Cst(TypeConstructor.SchemaRowEmpty, SourceLocation.Unknown)

  /**
    * Represents the Boolean True.
    */
  val True: Type = Type.Cst(TypeConstructor.True, SourceLocation.Unknown)

  /**
    * Represents the Boolean False.
    */
  val False: Type = Type.Cst(TypeConstructor.False, SourceLocation.Unknown)

  /**
    * Represents the Pure effect. (TRUE in the Boolean algebra.)
    */
  val Pure: Type = True

  /**
    * Represents the Impure effect. (FALSE in the Boolean algebra.)
    */
  val Impure: Type = False

  /**
    * Represents the Not type constructor.
    *
    * NB: This type has kind: * -> *.
    */
  val Not: Type = Type.Cst(TypeConstructor.Not, SourceLocation.Unknown)

  /**
    * Represents the And type constructor.
    *
    * NB: This type has kind: * -> (* -> *).
    */
  val And: Type = Type.Cst(TypeConstructor.And, SourceLocation.Unknown)

  /**
    * Represents the Or type constructor.
    *
    * NB: This type has kind: * -> (* -> *).
    */
  val Or: Type = Type.Cst(TypeConstructor.Or, SourceLocation.Unknown)

  /**
    * Represents the Empty effect type.
    */
  val Empty: Type = Type.Cst(TypeConstructor.Empty, SourceLocation.Unknown)

  /**
    * Represents the All effect type.
    */
  val All: Type = Type.Cst(TypeConstructor.All, SourceLocation.Unknown)

  /////////////////////////////////////////////////////////////////////////////
  // Constructors                                                            //
  /////////////////////////////////////////////////////////////////////////////

  /**
    * The union of non-Apply types.
    * Used to restrict the range of return values of [[Type.baseType]].
    */
  sealed trait BaseType extends Type

  /**
    * A type variable.
    */
  case class Var(sym: Symbol.KindedTypeVarSym, loc: SourceLocation) extends Type with BaseType with Ordered[Type.Var] {

    def withText(text: Ast.VarText): Var = Var(sym.withText(text), loc)

    def kind: Kind = sym.kind

    /**
      * Removes the kind from this var.
      */
    def withoutKind: UnkindedType.Var = UnkindedType.Var(sym.withoutKind, loc)

    /**
      * Returns `true` if `this` type variable is equal to `o`.
      */
    override def equals(o: scala.Any): Boolean = o match {
      case that: Var => this.sym == that.sym
      case _ => false
    }

    /**
      * Returns the hash code of `this` type variable.
      */
    override def hashCode(): Int = sym.hashCode

    /**
      * Compares `this` type variable to `that` type variable.
      */
    override def compare(that: Type.Var): Int = this.sym.id - that.sym.id
  }

  /**
    * A type represented by the type constructor `tc`.
    */
  case class Cst(tc: TypeConstructor, loc: SourceLocation) extends Type with BaseType {
    def kind: Kind = tc.kind

    override def hashCode(): Int = tc.hashCode()

    override def equals(o: Any): Boolean = o match {
      case that: Cst => this.tc == that.tc
      case _ => false
    }
  }

  /**
    * A type expression that a represents a type application tpe1[tpe2].
    */
  case class Apply(tpe1: Type, tpe2: Type, loc: SourceLocation) extends Type {
    /**
      * Returns the kind of `this` type.
      *
      * The kind of a type application can unique be determined from the kind of the first type argument `t1`.
      */
    lazy val kind: Kind = {
      tpe1.kind match {
        case Kind.Arrow(k1, k2) =>
          // TODO: Kind check (but only for boolean formulas for now).
          //          if (k1 == Kind.Bool) {
          //            val k3 = tpe2.kind
          //            if (k3 != Kind.Bool && !k3.isInstanceOf[Kind.Var]) {
          //               throw InternalCompilerException(s"Unexpected non-bool kind: '$k3'.")
          //            }
          //          }
          k2
        case _ => throw InternalCompilerException(s"Illegal kind: '${tpe1.kind}' of type '$tpe1'.", loc)
      }
    }

    override def equals(o: Any): Boolean = o match {
      case that: Apply => this.tpe1 == that.tpe1 && this.tpe2 == that.tpe2
      case _ => false
    }

    override def hashCode(): Int = Objects.hash(tpe1, tpe2)
  }

  /**
    * A type alias, including the arguments passed to it and the type it represents.
    */
  case class Alias(cst: Ast.AliasConstructor, args: List[Type], tpe: Type, loc: SourceLocation) extends Type with BaseType {
    override def kind: Kind = tpe.kind
  }

  /////////////////////////////////////////////////////////////////////////////
  // Utility Functions                                                       //
  /////////////////////////////////////////////////////////////////////////////

  /**
    * Returns a fresh type variable of the given kind `k` and rigidity `r`.
    */
  def freshVar(k: Kind, loc: SourceLocation, isRegion: Boolean = false, text: Ast.VarText = Ast.VarText.Absent)(implicit flix: Flix): Type.Var = {
    val sym = Symbol.freshKindedTypeVarSym(text, k, isRegion, loc)
    Type.Var(sym, loc)
  }

  /**
    * Returns the Unit type with given source location `loc`.
    */
  def mkUnit(loc: SourceLocation): Type = Type.Cst(TypeConstructor.Unit, loc)

  /**
    * Returns the Null type with the given source location `loc`.
    */
  def mkNull(loc: SourceLocation): Type = Type.Cst(TypeConstructor.Null, loc)

  /**
    * Returns the Bool type with the given source location `loc`.
    */
  def mkBool(loc: SourceLocation): Type = Type.Cst(TypeConstructor.Bool, loc)

  /**
    * Returns the Char type with the given source location `loc`.
    */
  def mkChar(loc: SourceLocation): Type = Type.Cst(TypeConstructor.Char, loc)

  /**
    * Returns the Float32 type with the given source location `loc`.
    */
  def mkFloat32(loc: SourceLocation): Type = Type.Cst(TypeConstructor.Float32, loc)

  /**
    * Returns the Float64 type with the given source location `loc`.
    */
  def mkFloat64(loc: SourceLocation): Type = Type.Cst(TypeConstructor.Float64, loc)

  /**
    * Returns the BigDecimal type with the given source location `loc`.
    */
  def mkBigDecimal(loc: SourceLocation): Type = Type.Cst(TypeConstructor.BigDecimal, loc)

  /**
    * Returns the Int8 type with the given source location `loc`.
    */
  def mkInt8(loc: SourceLocation): Type = Type.Cst(TypeConstructor.Int8, loc)

  /**
    * Returns the Int16 type with the given source location `loc`.
    */
  def mkInt16(loc: SourceLocation): Type = Type.Cst(TypeConstructor.Int16, loc)

  /**
    * Returns the Int32 type with the given source location `loc`.
    */
  def mkInt32(loc: SourceLocation): Type = Type.Cst(TypeConstructor.Int32, loc)

  /**
    * Returns the Int64 type with the given source location `loc`.
    */
  def mkInt64(loc: SourceLocation): Type = Type.Cst(TypeConstructor.Int64, loc)

  /**
    * Returns the BigInt type with the given source location `loc`.
    */
  def mkBigInt(loc: SourceLocation): Type = Type.Cst(TypeConstructor.BigInt, loc)

  /**
    * Returns the String type with the given source location `loc`.
    */
  def mkString(loc: SourceLocation): Type = Type.Cst(TypeConstructor.Str, loc)

  /**
    * Returns the True type with the given source location `loc`.
    */
  def mkTrue(loc: SourceLocation): Type = Type.Cst(TypeConstructor.True, loc)

  /**
    * Returns the False type with the given source location `loc`.
    */
  def mkFalse(loc: SourceLocation): Type = Type.Cst(TypeConstructor.False, loc)

  /**
    * Returns the type `Sender[tpe, reg]` with the given optional source location `loc`.
    */
  def mkSender(tpe: Type, reg: Type, loc: SourceLocation): Type =
    Apply(Apply(Cst(TypeConstructor.Sender, loc), tpe, loc), reg, loc)

  /**
    * Returns the type `Receiver[tpe, reg]` with the given optional source location `loc`.
    */
  def mkReceiver(tpe: Type, reg: Type, loc: SourceLocation): Type =
    Apply(Apply(Cst(TypeConstructor.Receiver, loc), tpe, loc), reg, loc)

  /**
    * Returns the Lazy type with the given source location `loc`.
    */
  def mkLazy(loc: SourceLocation): Type = Type.Cst(TypeConstructor.Lazy, loc)

  /**
    * Returns the type `Lazy[tpe]` with the given optional source location `loc`.
    */
  def mkLazy(tpe: Type, loc: SourceLocation): Type = Type.Apply(Type.Cst(TypeConstructor.Lazy, loc), tpe, loc)

  /**
    * Returns the type `Array[tpe, reg]` with the given source location `loc`.
    */
  def mkArray(tpe: Type, reg: Type, loc: SourceLocation): Type =
    Apply(Apply(Cst(TypeConstructor.Array, loc), tpe, loc), reg, loc)

  /**
    * Returns the type `Ref[tpe, reg]` with the given source location `loc`.
    */
  def mkRef(tpe: Type, reg: Type, loc: SourceLocation): Type =
    Apply(Apply(Cst(TypeConstructor.Ref, loc), tpe, loc), reg, loc)

  /**
    * Constructs the pure arrow type A -> B.
    */
  def mkPureArrow(a: Type, b: Type, loc: SourceLocation): Type = mkArrowWithEffect(a, Pure, Empty, b, loc)

  /**
    * Constructs the impure arrow type A ~> B.
    */
  def mkImpureArrow(a: Type, b: Type, loc: SourceLocation): Type = mkArrowWithEffect(a, Impure, Empty, b, loc)

  /**
    * Constructs the arrow type A -> B & e.
    */
  def mkArrowWithEffect(a: Type, p: Type, e: Type, b: Type, loc: SourceLocation): Type = mkApply(Type.Cst(TypeConstructor.Arrow(2), loc), List(p, e, a, b), loc)

  /**
    * Constructs the pure curried arrow type A_1 -> (A_2  -> ... -> A_n) -> B.
    */
  def mkPureCurriedArrow(as: List[Type], b: Type, loc: SourceLocation): Type = mkCurriedArrowWithEffect(as, Pure, Empty, b, loc)

  /**
    * Constructs the impure curried arrow type A_1 -> (A_2  -> ... -> A_n) ~> B.
    */
  def mkImpureCurriedArrow(as: List[Type], b: Type, loc: SourceLocation): Type = mkCurriedArrowWithEffect(as, Impure, Empty, b, loc)

  /**
    * Constructs the curried arrow type A_1 -> (A_2  -> ... -> A_n) -> B & e.
    */
  def mkCurriedArrowWithEffect(as: List[Type], p: Type, e: Type, b: Type, loc: SourceLocation): Type = {
    val a = as.last
    val base = mkArrowWithEffect(a, p, e, b, loc)
    as.init.foldRight(base)(mkPureArrow(_, _, loc))
  }

  /**
    * Constructs the pure uncurried arrow type (A_1, ..., A_n) -> B.
    */
  def mkPureUncurriedArrow(as: List[Type], b: Type, loc: SourceLocation): Type = mkUncurriedArrowWithEffect(as, Pure, Empty, b, loc)

  /**
    * Constructs the impure uncurried arrow type (A_1, ..., A_n) ~> B.
    */
  def mkImpureUncurriedArrow(as: List[Type], b: Type, loc: SourceLocation): Type = mkUncurriedArrowWithEffect(as, Impure, Empty, b, loc)

  /**
    * Constructs the uncurried arrow type (A_1, ..., A_n) -> B & e.
    */
  def mkUncurriedArrowWithEffect(as: List[Type], p: Type, e: Type, b: Type, loc: SourceLocation): Type = {
    val arrow = mkApply(Type.Cst(TypeConstructor.Arrow(as.length + 1), loc), List(p, e), loc)
    val inner = as.foldLeft(arrow: Type) {
      case (acc, x) => Apply(acc, x, loc)
    }
    Apply(inner, b, loc)
  }

  /**
    * Constructs the apply type base[t_1, ,..., t_n].
    */
  def mkApply(base: Type, ts: List[Type], loc: SourceLocation): Type = ts.foldLeft(base) {
    case (acc, t) => Apply(acc, t, loc)
  }

  /**
    * Returns the type `Choice[tpe, isAbsent, isPresent]`.
    */
  def mkChoice(tpe0: Type, isAbsent: Type, isPresent: Type, loc: SourceLocation): Type = {
    val sym = Symbol.mkEnumSym("Choice")
    val kind = Kind.Star ->: Kind.Bool ->: Kind.Bool ->: Kind.Star
    val tc = TypeConstructor.Enum(sym, kind)
    Apply(Apply(Apply(Cst(tc, loc), tpe0, loc), isAbsent, loc), isPresent, loc)
  }

  /**
    * Construct the enum type constructor for the given symbol `sym` with the given kind `k`.
    */
  def mkEnum(sym: Symbol.EnumSym, k: Kind, loc: SourceLocation): Type = Type.Cst(TypeConstructor.Enum(sym, k), loc)

  /**
    * Construct the enum type `Sym[ts]`.
    */
  def mkEnum(sym: Symbol.EnumSym, ts: List[Type], loc: SourceLocation): Type = mkApply(Type.Cst(TypeConstructor.Enum(sym, Kind.mkArrow(ts.length)), loc), ts, loc)

  /**
    * Constructs the tuple type (A, B, ...) where the types are drawn from the list `ts`.
    */
  def mkTuple(ts: List[Type], loc: SourceLocation): Type = {
    val init = Type.Cst(TypeConstructor.Tuple(ts.length), loc)
    ts.foldLeft(init: Type) {
      case (acc, x) => Apply(acc, x, loc)
    }
  }

  /**
    * Constructs the a native type.
    */
  def mkNative(clazz: Class[_], loc: SourceLocation): Type = Type.Cst(TypeConstructor.Native(clazz), loc)

  /**
    * Constructs a RecordExtend type.
    */
  def mkRecordRowEmpty(loc: SourceLocation): Type = {
    Type.Cst(TypeConstructor.RecordRowEmpty, loc)
  }

  /**
    * Constructs a SchemaExtend type.
    */
  def mkSchemaRowEmpty(loc: SourceLocation): Type = {
    Type.Cst(TypeConstructor.SchemaRowEmpty, loc)
  }

  /**
    * Constructs a RecordExtend type.
    */
  def mkRecordRowExtend(field: Name.Field, tpe: Type, rest: Type, loc: SourceLocation): Type = {
    mkApply(Type.Cst(TypeConstructor.RecordRowExtend(field), loc), List(tpe, rest), loc)
  }

  /**
    * Constructs a SchemaExtend type.
    */
  def mkSchemaRowExtend(pred: Name.Pred, tpe: Type, rest: Type, loc: SourceLocation): Type = {
    mkApply(Type.Cst(TypeConstructor.SchemaRowExtend(pred), loc), List(tpe, rest), loc)
  }

  /**
    * Constructs a Record type.
    */
  def mkRecord(tpe: Type, loc: SourceLocation): Type = {
    Apply(Type.Cst(TypeConstructor.Record, loc), tpe, loc)
  }

  /**
    * Constructs a Schema type.
    */
  def mkSchema(tpe: Type, loc: SourceLocation): Type = {
    Apply(Type.Cst(TypeConstructor.Schema, loc), tpe, loc)
  }

  /**
    * Construct a relation type with the given list of type arguments `ts0`.
    */
  def mkRelation(ts0: List[Type], loc: SourceLocation): Type = {
    val ts = ts0 match {
      case Nil => Type.Unit
      case x :: Nil => x
      case xs => mkTuple(xs, loc)
    }

    Apply(Relation, ts, loc)
  }

  /**
    * Construct a lattice type with the given list of type arguments `ts0`.
    */
  def mkLattice(ts0: List[Type], loc: SourceLocation): Type = {
    val ts = ts0 match {
      case Nil => Type.Unit
      case x :: Nil => x
      case xs => mkTuple(xs, loc)
    }

    Apply(Lattice, ts, loc)
  }

  /**
    * Returns the type `Not(tpe0)`.
    */
  def mkNot(tpe0: Type, loc: SourceLocation): Type = tpe0 match {
    case Type.True => Type.mkFalse(loc)
    case Type.False => Type.mkTrue(loc)
    case _ => Type.Apply(Type.Cst(TypeConstructor.Not, loc), tpe0, loc)
  }

  /**
    * Returns the type `And(tpe1, tpe2)`.
    *
    * Must not be used before kinding.
    */
  def mkAnd(tpe1: Type, tpe2: Type, loc: SourceLocation): Type = (tpe1, tpe2) match {
    case (Type.Cst(TypeConstructor.True, _), _) => tpe2
    case (_, Type.Cst(TypeConstructor.True, _)) => tpe1
    case (Type.Cst(TypeConstructor.False, _), _) => Type.False
    case (_, Type.Cst(TypeConstructor.False, _)) => Type.False
    case (Type.Var(sym1, _), Type.Var(sym2, _)) if sym1 == sym2 => tpe1
    case _ => Type.Apply(Type.Apply(Type.And, tpe1, loc), tpe2, loc)
  }

  /**
    * Returns the type `And(tpe1, And(tpe2, tpe3))`.
    *
    * Must not be used before kinding.
    */
  def mkAnd(tpe1: Type, tpe2: Type, tpe3: Type, loc: SourceLocation): Type =
    mkAnd(tpe1, mkAnd(tpe2, tpe3, loc), loc)

  /**
    * Returns the type `And(tpe1, And(tpe2, And(tpe3, tpe4)))`.
    *
    * Must not be used before kinding.
    */
  def mkAnd(tpe1: Type, tpe2: Type, tpe3: Type, tpe4: Type, loc: SourceLocation): Type =
    mkAnd(tpe1, mkAnd(tpe2, mkAnd(tpe3, tpe4, loc), loc), loc)

  /**
    * Returns the type `And(tpe1, And(tpe2, ...))`.
    *
    * Must not be used before kinding.
    */
  def mkAnd(tpes: List[Type], loc: SourceLocation): Type = tpes match {
    case Nil => Type.True
    case x :: xs => mkAnd(x, mkAnd(xs, loc), loc)
  }

  /**
    * Returns the type `Or(tpe1, tpe2)`.
    *
    * Must not be used before kinding.
    */
  def mkOr(tpe1: Type, tpe2: Type, loc: SourceLocation): Type = (tpe1, tpe2) match {
    case (Type.Cst(TypeConstructor.True, _), _) => Type.True
    case (_, Type.Cst(TypeConstructor.True, _)) => Type.True
    case (Type.Cst(TypeConstructor.False, _), _) => tpe2
    case (_, Type.Cst(TypeConstructor.False, _)) => tpe1
    case (Type.Var(sym1, _), Type.Var(sym2, _)) if sym1 == sym2 => tpe1
    case _ => Type.Apply(Type.Apply(Type.Or, tpe1, loc), tpe2, loc)
  }

  /**
    * Returns the type `Or(tpe1, Or(tpe2, ...))`.
    *
    * Must not be used before kinding.
    */
  def mkOr(tpes: List[Type], loc: SourceLocation): Type = tpes match {
    case Nil => Type.False
    case x :: xs => mkOr(x, mkOr(xs, loc), loc)
  }

  /**
    * Returns the complement of the given type.
    *
    * Must not be used before kinding.
    */
  def mkComplement(tpe: Type, loc: SourceLocation): Type = tpe match {
    case Type.Empty => Type.All
    case Type.All => Type.Empty
    case t => Type.Apply(Type.Cst(TypeConstructor.Complement, loc), t, loc)
  }

  /**
    * Returns the type `tpe1 + tpe2`
    *
    * Must not be used before kinding.
    */
  def mkUnion(tpe1: Type, tpe2: Type, loc: SourceLocation): Type = (tpe1, tpe2) match {
    case (Empty, t) => t
    case (t, Empty) => t
    case (All, t) => All
    case (t, All) => All
    case _ => mkApply(Type.Cst(TypeConstructor.Union, loc), List(tpe1, tpe2), loc)
  }

  /**
    * Returns the union of all the given types.
    *
    * Must not be used before kinding.
    */
  def mkUnion(tpes: List[Type], loc: SourceLocation): Type = tpes match {
    case Nil => Type.Empty
    case x :: xs => mkUnion(x, mkUnion(xs, loc), loc)
  }

  /**
    * Returns the type `tpe1 & tpe2`
    *
    * Must not be used before kinding.
    */
  def mkIntersection(tpe1: Type, tpe2: Type, loc: SourceLocation): Type = (tpe1, tpe2) match {
    case (Empty, _) => Empty
    case (_, Empty) => Empty
    case (All, t) => t
    case (t, All) => t
    case _ => mkApply(Type.Cst(TypeConstructor.Intersection, loc), List(tpe1, tpe2), loc)
  }

  /**
    * Returns the intersection of all the given types.
    *
    * Must not be used before kinding.
    */
  def mkIntersection(tpes: List[Type], loc: SourceLocation): Type = tpes match {
    case Nil => Type.All
    case x :: xs => mkIntersection(x, mkIntersection(xs, loc), loc)
  }

  /**
    * Returns the difference of the given types.
    *
    * Must not be used before kinding.
    */
  def mkDifference(tpe1: Type, tpe2: Type, loc: SourceLocation): Type = mkIntersection(tpe1, mkComplement(tpe2, loc), loc)

  /**
    * Returns a Region type for the given region argument `r` with the given source location `loc`.
    */
  def mkRegion(r: Type, loc: SourceLocation): Type =
    Type.Apply(Type.Cst(TypeConstructor.RegionToStar, loc), r, loc)

  /**
    * Returns the type `tpe1 => tpe2`.
    */
  def mkImplies(tpe1: Type, tpe2: Type, loc: SourceLocation): Type = mkOr(Type.mkNot(tpe1, loc), tpe2, loc)

  /**
    * Returns a Boolean type that represents the equivalence of `x` and `y`.
    *
    * That is, `x == y` iff `(x /\ y) \/ (not x /\ not y)`
    */
  def mkEquiv(x: Type, y: Type, loc: SourceLocation): Type = mkOr(mkAnd(x, y, loc), mkAnd(Type.mkNot(x, loc), Type.mkNot(y, loc), loc), loc)

  /**
    * Replace type aliases with the types they represent.
    * In general, this function should be used in back-end phases
    * to clear all aliases for easier processing.
    */
  def eraseAliases(t: Type): Type = t match {
    case tvar: Type.Var => tvar
    case Type.Cst(_, _) => t
    case Type.Apply(tpe1, tpe2, loc) => Type.Apply(eraseAliases(tpe1), eraseAliases(tpe2), loc)
    case Type.Alias(_, _, tpe, _) => eraseAliases(tpe)
  }

  /**
    * Replace top-level type aliases with the types they represent.
    * In general, this function should be used in front-end phases
    * to maintain aliases in error messages to the extent possible.
    */
  @tailrec
  def eraseTopAliases(t: Type): Type = t match {
    case Type.Alias(_, _, tpe, _) => eraseTopAliases(tpe)
    case tpe => tpe
  }

  /**
    * Returns the Flix Type of a Java Class
    */
  def getFlixType(c: Class[_]): Type = {
    if (c == java.lang.Boolean.TYPE) {
      Type.Bool
    }
    else if (c == java.lang.Byte.TYPE) {
      Type.Int8
    }
    else if (c == java.lang.Short.TYPE) {
      Type.Int16
    }
    else if (c == java.lang.Integer.TYPE) {
      Type.Int32
    }
    else if (c == java.lang.Long.TYPE) {
      Type.Int64
    }
    else if (c == java.lang.Character.TYPE) {
      Type.Char
    }
    else if (c == java.lang.Float.TYPE) {
      Type.Float32
    }
    else if (c == java.lang.Double.TYPE) {
      Type.Float64
    }
    else if (c == classOf[java.math.BigDecimal]) {
      Type.BigDecimal
    }
    else if (c == classOf[java.math.BigInteger]) {
      Type.BigInt
    }
    else if (c == classOf[java.lang.String]) {
      Type.Str
    }
    else if (c == java.lang.Void.TYPE) {
      Type.Unit
    }
    // handle arrays of types
    else if (c.isArray) {
      val comp = c.getComponentType
      val elmType = getFlixType(comp)
      Type.mkArray(elmType, Type.False, SourceLocation.Unknown)
    }
    // otherwise native type
    else {
      Type.mkNative(c, SourceLocation.Unknown)
    }
  }

}
