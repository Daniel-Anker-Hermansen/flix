/*
 * Copyright 2022 Matthew Lutze
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
import ca.uwaterloo.flix.language.ast.Ast.EliminatedBy
import ca.uwaterloo.flix.language.phase.Resolver
import ca.uwaterloo.flix.util.InternalCompilerException

import java.util.Objects
import scala.collection.immutable.SortedSet

sealed trait UnkindedType {
  def loc: SourceLocation

  /**
    * Maps all the type vars in the type according to the given function `f`.
    */
  def map(f: Symbol.UnkindedTypeVarSym => UnkindedType): UnkindedType = this match {
    case UnkindedType.Var(sym, _) => f(sym)
    case t: UnkindedType.Cst => t
    case t: UnkindedType.Enum => t
    case t: UnkindedType.UnappliedAlias => t
    case UnkindedType.Apply(tpe1, tpe2, loc) => UnkindedType.Apply(tpe1.map(f), tpe2.map(f), loc)
    case UnkindedType.Arrow(purAndEff, arity, loc) => UnkindedType.Arrow(purAndEff.map(_.map(f)), arity, loc)
    case UnkindedType.ReadWrite(tpe, loc) => UnkindedType.ReadWrite(tpe.map(f), loc)
    case UnkindedType.Ascribe(tpe, kind, loc) => UnkindedType.Ascribe(tpe.map(f), kind, loc)
    case UnkindedType.Alias(cst, args, tpe, loc) => UnkindedType.Alias(cst, args.map(_.map(f)), tpe.map(f), loc)
  }

  /**
    * Returns the base type.
    *
    * For example,
    * X[a, b, c] returns X
    *
    */
  def baseType: UnkindedType = this match {
    case UnkindedType.Apply(tpe1, _, _) => tpe1.baseType
    case t => t
  }

  /**
    * Returns the type arguments.
    *
    * For example
    * X[a, b, c] returns [a, b, c]
    */
  def typeArguments: List[UnkindedType] = this match {
    case UnkindedType.Apply(tpe1, tpe2, _) => tpe1.typeArguments :+ tpe2
    case _ => Nil
  }

  /**
    * Returns the type variables in the given type.
    */
  def typeVars: SortedSet[Symbol.UnkindedTypeVarSym] = this match {
    case UnkindedType.Var(sym, loc) => SortedSet(sym)
    case UnkindedType.Cst(tc, loc) => SortedSet.empty
    case UnkindedType.Enum(sym, loc) => SortedSet.empty
    case UnkindedType.UnappliedAlias(sym, loc) => SortedSet.empty
    case UnkindedType.Apply(tpe1, tpe2, loc) => tpe1.typeVars ++ tpe2.typeVars
    case UnkindedType.Arrow(UnkindedType.PurityAndEffect(pur, eff), arity, loc) =>
      val p = pur.iterator.flatMap(_.typeVars).to(SortedSet)
      val e = eff.iterator.flatMap(_.flatMap(_.typeVars)).to(SortedSet)
      p ++ e
    case UnkindedType.ReadWrite(tpe, loc) => tpe.typeVars
    case UnkindedType.Ascribe(tpe, kind, loc) => tpe.typeVars
    case UnkindedType.Alias(cst, args, tpe, loc) => args.flatMap(_.typeVars).to(SortedSet)
  }
}

object UnkindedType {

  /**
    * A type variable.
    */
  case class Var(sym: Symbol.UnkindedTypeVarSym, loc: SourceLocation) extends UnkindedType {
    override def equals(that: Any): Boolean = that match {
      case Var(sym2, _) => sym == sym2
      case _ => false
    }

    override def hashCode(): Int = Objects.hash(sym)
  }

  /**
    * A type constant.
    */
  case class Cst(tc: TypeConstructor, loc: SourceLocation) extends UnkindedType {
    override def equals(that: Any): Boolean = that match {
      case Cst(tc2, _) => tc == tc2
      case _ => false
    }

    override def hashCode(): Int = Objects.hash(tc)
  }

  /**
    * An unkinded enum.
    */
  case class Enum(sym: Symbol.EnumSym, loc: SourceLocation) extends UnkindedType {
    override def equals(that: Any): Boolean = that match {
      case Enum(sym2, _) => sym == sym2
      case _ => false
    }

    override def hashCode(): Int = Objects.hash(sym)
  }

  /**
    * An unapplied alias.
    * Only exists temporarily in the Resolver until it's converted to an [[Alias]].
    */
  @EliminatedBy(Resolver.getClass)
  case class UnappliedAlias(sym: Symbol.TypeAliasSym, loc: SourceLocation) extends UnkindedType {
    override def equals(that: Any): Boolean = that match {
      case UnappliedAlias(sym2, _) => sym == sym2
      case _ => false
    }

    override def hashCode(): Int = Objects.hash(sym)
  }

  /**
    * A type application.
    */
  case class Apply(tpe1: UnkindedType, tpe2: UnkindedType, loc: SourceLocation) extends UnkindedType {
    override def equals(that: Any): Boolean = that match {
      case Apply(tpe1_2, tpe2_2, _) => tpe1 == tpe1_2 && tpe2 == tpe2_2
      case _ => false
    }

    override def hashCode(): Int = Objects.hash(tpe1, tpe2)
  }

  /**
    * A function type.
    */
  case class Arrow(purAndEff: PurityAndEffect, arity: Int, loc: SourceLocation) extends UnkindedType {
    override def equals(that: Any): Boolean = that match {
      case Arrow(purAndEff2, arity2, _) => purAndEff2 == purAndEff && arity == arity2
      case _ => false
    }

    override def hashCode(): Int = Objects.hash(purAndEff, arity)
  }

  /**
    * A read or write type.
    */
  case class ReadWrite(tpe: UnkindedType, loc: SourceLocation) extends UnkindedType {
    override def equals(that: Any): Boolean = that match {
      case ReadWrite(tpe2, _) => tpe == tpe2
      case _ => false
    }

    override def hashCode(): Int = Objects.hash(tpe)
  }

  /**
    * A type with a kind ascription.
    */
  case class Ascribe(tpe: UnkindedType, kind: Kind, loc: SourceLocation) extends UnkindedType {
    override def equals(that: Any): Boolean = that match {
      case Ascribe(tpe2, kind2, _) => tpe == tpe2 && kind == kind2
      case _ => false
    }

    override def hashCode(): Int = Objects.hash(tpe, kind)
  }

  /**
    * A fully resolved type alias.
    */
  case class Alias(cst: Ast.AliasConstructor, args: List[UnkindedType], tpe: UnkindedType, loc: SourceLocation) extends UnkindedType {
    override def equals(that: Any): Boolean = that match {
      case Alias(Ast.AliasConstructor(sym2, _), args2, tpe2, _) => cst.sym == sym2 && args == args2 && tpe == tpe2
      case _ => false
    }

    override def hashCode(): Int = Objects.hash(cst, args, tpe)
  }

  case class PurityAndEffect(pur: Option[UnkindedType], eff: Option[List[UnkindedType]]) {
    /**
      * Maps the function `f` over the contents of this PurityAndEffect
      */
    def map(f: UnkindedType => UnkindedType): PurityAndEffect = PurityAndEffect(pur.map(f), eff.map(_.map(f)))
  }

  /**
    * Returns a fresh type variable of the given kind `k` and rigidity `r`.
    */
  def freshVar(loc: SourceLocation, isRegion: Boolean = false, text: Ast.VarText = Ast.VarText.Absent)(implicit flix: Flix): UnkindedType.Var = {
    val sym = Symbol.freshUnkindedTypeVarSym(text, isRegion, loc)
    UnkindedType.Var(sym, loc)
  }

  /**
    * Returns the Int32 type.
    */
  def mkInt32(loc: SourceLocation): UnkindedType = {
    UnkindedType.Cst(TypeConstructor.Int32, loc)
  }

  /**
    * Returns the Int64 type.
    */
  def mkInt64(loc: SourceLocation): UnkindedType = {
    UnkindedType.Cst(TypeConstructor.Int64, loc)
  }

  /**
    * Returns the Float64 type.
    */
  def mkFloat64(loc: SourceLocation): UnkindedType = {
    UnkindedType.Cst(TypeConstructor.Float64, loc)
  }

  /**
   * Returns the Bool type.
   */
  def mkBool(loc: SourceLocation): UnkindedType = {
    UnkindedType.Cst(TypeConstructor.Bool, loc)
  }

  /**
   * Returns the Unit type.
   */
  def mkUnit(loc: SourceLocation): UnkindedType = {
    UnkindedType.Cst(TypeConstructor.Unit, loc)
  }

  /**
    * Returns the ##java.lang.Object type.
    */
  def mkObject(loc: SourceLocation): UnkindedType = {
    val obj = Class.forName("java.lang.Object")
    UnkindedType.Cst(TypeConstructor.Native(obj), loc)
  }

  /**
    * Constructs the apply type base[t_1, ,..., t_n].
    */
  def mkApply(base: UnkindedType, ts: List[UnkindedType], loc: SourceLocation): UnkindedType = ts.foldLeft(base) {
    case (acc, t) => Apply(acc, t, loc)
  }

  /**
    * Constructs the type a -> b \ IO
    */
  def mkImpureArrow(a: UnkindedType, b: UnkindedType, loc: SourceLocation): UnkindedType = {
    val purAndEff = PurityAndEffect(Some(UnkindedType.Cst(TypeConstructor.False, loc)), None)
    mkApply(UnkindedType.Arrow(purAndEff, 2, loc), List(a, b), loc)
  }

  /**
    * Constructs the tuple type (A, B, ...) where the types are drawn from the list `ts`.
    */
  def mkTuple(ts: List[UnkindedType], loc: SourceLocation): UnkindedType = {
    val init = UnkindedType.Cst(TypeConstructor.Tuple(ts.length), loc)
    ts.foldLeft(init: UnkindedType) {
      case (acc, x) => Apply(acc, x, loc)
    }
  }

  /**
    * Constructs a RecordExtend type.
    */
  def mkRecordRowExtend(field: Name.Field, tpe: UnkindedType, rest: UnkindedType, loc: SourceLocation): UnkindedType = {
    mkApply(UnkindedType.Cst(TypeConstructor.RecordRowExtend(field), loc), List(tpe, rest), loc)
  }

  /**
    * Constructs a SchemaExtend type.
    */
  def mkSchemaRowExtend(pred: Name.Pred, tpe: UnkindedType, rest: UnkindedType, loc: SourceLocation): UnkindedType = {
    mkApply(UnkindedType.Cst(TypeConstructor.SchemaRowExtend(pred), loc), List(tpe, rest), loc)
  }

  /**
    * Constructs a Record type.
    */
  def mkRecord(tpe: UnkindedType, loc: SourceLocation): UnkindedType = {
    Apply(UnkindedType.Cst(TypeConstructor.Record, loc), tpe, loc)
  }

  /**
    * Constructs a Schema type.
    */
  def mkSchema(tpe: UnkindedType, loc: SourceLocation): UnkindedType = {
    Apply(UnkindedType.Cst(TypeConstructor.Schema, loc), tpe, loc)
  }


  /**
    * Construct a relation type with the given list of type arguments `ts0`.
    */
  def mkRelation(ts0: List[UnkindedType], loc: SourceLocation): UnkindedType = {
    val ts = ts0 match {
      case Nil => UnkindedType.Cst(TypeConstructor.Unit, loc)
      case x :: Nil => x
      case xs => mkTuple(xs, loc)
    }

    Apply(UnkindedType.Cst(TypeConstructor.Relation, loc), ts, loc)
  }

  /**
    * Construct a lattice type with the given list of type arguments `ts0`.
    */
  def mkLattice(ts0: List[UnkindedType], loc: SourceLocation): UnkindedType = {
    val ts = ts0 match {
      case Nil => UnkindedType.Cst(TypeConstructor.Unit, loc)
      case x :: Nil => x
      case xs => mkTuple(xs, loc)
    }

    Apply(UnkindedType.Cst(TypeConstructor.Lattice, loc), ts, loc)
  }

  /**
    * Construct the enum type constructor for the given symbol `sym` with the given kind `k`.
    */
  def mkEnum(sym: Symbol.EnumSym, loc: SourceLocation): UnkindedType = UnkindedType.Enum(sym, loc)

  /**
    * Construct the effect type for the given symbol.
    */
  def mkEffect(sym: Symbol.EffectSym, loc: SourceLocation): UnkindedType = UnkindedType.Cst(TypeConstructor.Effect(sym), loc)

  /**
    * Constructs a predicate type.
    */
  def mkPredicate(den: Ast.Denotation, ts0: List[UnkindedType], loc: SourceLocation): UnkindedType = {
    val tycon = den match {
      case Ast.Denotation.Relational => UnkindedType.Cst(TypeConstructor.Relation, loc)
      case Ast.Denotation.Latticenal => UnkindedType.Cst(TypeConstructor.Lattice, loc)
    }
    val ts = ts0 match {
      case Nil => UnkindedType.Cst(TypeConstructor.Unit, loc)
      case x :: Nil => x
      case xs => UnkindedType.mkTuple(xs, loc)
    }

    UnkindedType.Apply(tycon, ts, loc)
  }

  /**
    * Returns the type `Not(tpe1)`.
    */
  def mkNot(tpe1: UnkindedType, loc: SourceLocation): UnkindedType = UnkindedType.mkApply(UnkindedType.Cst(TypeConstructor.Not, loc), List(tpe1), loc)

  /**
    * Returns the type `And(tpe1, tpe2)`.
    */
  def mkAnd(tpe1: UnkindedType, tpe2: UnkindedType, loc: SourceLocation): UnkindedType = UnkindedType.mkApply(UnkindedType.Cst(TypeConstructor.And, loc), List(tpe1, tpe2), loc)

  /**
    * Returns the type `Or(tpe1, tpe2)`.
    */
  def mkOr(tpe1: UnkindedType, tpe2: UnkindedType, loc: SourceLocation): UnkindedType = UnkindedType.mkApply(UnkindedType.Cst(TypeConstructor.Or, loc), List(tpe1, tpe2), loc)

  /**
    * Returns the type `Complement(tpe1)`.
    */
  def mkComplement(tpe1: UnkindedType, loc: SourceLocation): UnkindedType = UnkindedType.mkApply(UnkindedType.Cst(TypeConstructor.Complement, loc), List(tpe1), loc)

  /**
    * Returns the type `Union(tpe1, tpe2)`.
    */
  def mkUnion(tpe1: UnkindedType, tpe2: UnkindedType, loc: SourceLocation): UnkindedType = UnkindedType.mkApply(UnkindedType.Cst(TypeConstructor.Union, loc), List(tpe1, tpe2), loc)

  /**
    * Returns the type `Intersection(tpe1, tpe2)`.
    */
  def mkIntersection(tpe1: UnkindedType, tpe2: UnkindedType, loc: SourceLocation): UnkindedType = UnkindedType.mkApply(UnkindedType.Cst(TypeConstructor.Intersection, loc), List(tpe1, tpe2), loc)

  /**
    * Constructs the uncurried arrow type (A_1, ..., A_n) -> B & e.
    */
  def mkUncurriedArrowWithEffect(as: List[UnkindedType], e: UnkindedType.PurityAndEffect, b: UnkindedType, loc: SourceLocation): UnkindedType = {
    val arrow = UnkindedType.Arrow(e, as.length + 1, loc)
    val inner = as.foldLeft(arrow: UnkindedType) {
      case (acc, x) => UnkindedType.Apply(acc, x, loc)
    }
    UnkindedType.Apply(inner, b, loc)
  }

  /**
    * Erases all the aliases from the type.
    */
  def eraseAliases(tpe0: UnkindedType): UnkindedType = tpe0 match {
    case tpe: Var => tpe
    case tpe: Cst => tpe
    case tpe: Enum => tpe
    case Apply(tpe1, tpe2, loc) => Apply(eraseAliases(tpe1), eraseAliases(tpe2), loc)
    case Arrow(purAndEff, arity, loc) => Arrow(purAndEff.map(eraseAliases), arity, loc)
    case ReadWrite(tpe, loc) => ReadWrite(eraseAliases(tpe), loc)
    case Ascribe(tpe, kind, loc) => Ascribe(eraseAliases(tpe), kind, loc)
    case Alias(_, _, tpe, _) => eraseAliases(tpe)
    case UnappliedAlias(_, loc) => throw InternalCompilerException("unexpected unapplied alias", loc)
  }

  // TODO remove once typechecking Resolver.lookupJVMMethod is moved to Typer

  /**
    * Returns the Flix UnkindedType of a Java Class
    */
  def getFlixType(c: Class[_]): UnkindedType = {
    if (c == java.lang.Boolean.TYPE) {
      UnkindedType.Cst(TypeConstructor.Bool, SourceLocation.Unknown)
    }
    else if (c == java.lang.Byte.TYPE) {
      UnkindedType.Cst(TypeConstructor.Int8, SourceLocation.Unknown)
    }
    else if (c == java.lang.Short.TYPE) {
      UnkindedType.Cst(TypeConstructor.Int16, SourceLocation.Unknown)
    }
    else if (c == java.lang.Integer.TYPE) {
      UnkindedType.Cst(TypeConstructor.Int32, SourceLocation.Unknown)
    }
    else if (c == java.lang.Long.TYPE) {
      UnkindedType.Cst(TypeConstructor.Int64, SourceLocation.Unknown)
    }
    else if (c == java.lang.Character.TYPE) {
      UnkindedType.Cst(TypeConstructor.Char, SourceLocation.Unknown)
    }
    else if (c == java.lang.Float.TYPE) {
      UnkindedType.Cst(TypeConstructor.Float32, SourceLocation.Unknown)
    }
    else if (c == java.lang.Double.TYPE) {
      UnkindedType.Cst(TypeConstructor.Float64, SourceLocation.Unknown)
    }
    else if (c == classOf[java.math.BigDecimal]) {
      UnkindedType.Cst(TypeConstructor.BigDecimal, SourceLocation.Unknown)
    }
    else if (c == classOf[java.math.BigInteger]) {
      UnkindedType.Cst(TypeConstructor.BigInt, SourceLocation.Unknown)
    }
    else if (c == classOf[java.lang.String]) {
      UnkindedType.Cst(TypeConstructor.Str, SourceLocation.Unknown)
    }
    else if (c == java.lang.Void.TYPE) {
      UnkindedType.Cst(TypeConstructor.Unit, SourceLocation.Unknown)
    }
    // handle arrays of types
    else if (c.isArray) {
      val comp = c.getComponentType
      val elmType = getFlixType(comp)
      UnkindedType.mkApply(
        UnkindedType.Cst(TypeConstructor.Array, SourceLocation.Unknown),
        List(elmType, UnkindedType.Cst(TypeConstructor.False, SourceLocation.Unknown)),
        SourceLocation.Unknown
      )
    }
    // otherwise native type
    else {
      UnkindedType.Cst(TypeConstructor.Native(c), SourceLocation.Unknown)
    }
  }
}
