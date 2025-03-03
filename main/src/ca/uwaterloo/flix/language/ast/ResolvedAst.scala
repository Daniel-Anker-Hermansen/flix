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

package ca.uwaterloo.flix.language.ast

import ca.uwaterloo.flix.language.ast.Ast.{Denotation, Source}
import ca.uwaterloo.flix.util.collection.MultiMap

import java.lang.reflect.{Constructor, Field, Method}

object ResolvedAst {

  case class Root(classes: Map[Symbol.ClassSym, ResolvedAst.Class],
                  instances: Map[Symbol.ClassSym, List[ResolvedAst.Instance]],
                  defs: Map[Symbol.DefnSym, ResolvedAst.Def],
                  enums: Map[Symbol.EnumSym, ResolvedAst.Enum],
                  effects: Map[Symbol.EffectSym, ResolvedAst.Effect],
                  typeAliases: Map[Symbol.TypeAliasSym, ResolvedAst.TypeAlias],
                  uses: Map[Symbol.ModuleSym, List[Ast.UseOrImport]],
                  taOrder: List[Symbol.TypeAliasSym],
                  entryPoint: Option[Symbol.DefnSym],
                  sources: Map[Source, SourceLocation],
                  names: MultiMap[List[String], String])

  // TODO use ResolvedAst.Law for laws
  case class CompilationUnit(usesAndImports: List[Ast.UseOrImport], decls: List[Declaration], loc: SourceLocation)
  case class Namespace(sym: Symbol.ModuleSym, usesAndImports: List[Ast.UseOrImport], decls: List[Declaration], loc: SourceLocation) extends Declaration
  case class Class(doc: Ast.Doc, ann: List[ResolvedAst.Annotation], mod: Ast.Modifiers, sym: Symbol.ClassSym, tparam: ResolvedAst.TypeParam, superClasses: List[ResolvedAst.TypeConstraint], sigs: Map[Symbol.SigSym, ResolvedAst.Sig], laws: List[ResolvedAst.Def], loc: SourceLocation) extends Declaration

  case class Instance(doc: Ast.Doc, ann: List[ResolvedAst.Annotation], mod: Ast.Modifiers, sym: Symbol.InstanceSym, tpe: UnkindedType, tconstrs: List[ResolvedAst.TypeConstraint], defs: List[ResolvedAst.Def], ns: Name.NName, loc: SourceLocation) extends Declaration

  case class Sig(sym: Symbol.SigSym, spec: ResolvedAst.Spec, exp: Option[ResolvedAst.Expression]) extends Declaration

  case class Def(sym: Symbol.DefnSym, spec: ResolvedAst.Spec, exp: ResolvedAst.Expression) extends Declaration

  case class Spec(doc: Ast.Doc, ann: List[ResolvedAst.Annotation], mod: Ast.Modifiers, tparams: ResolvedAst.TypeParams, fparams: List[ResolvedAst.FormalParam], tpe: UnkindedType, purAndEff: UnkindedType.PurityAndEffect, tconstrs: List[ResolvedAst.TypeConstraint], loc: SourceLocation)

  case class Enum(doc: Ast.Doc, ann: List[ResolvedAst.Annotation], mod: Ast.Modifiers, sym: Symbol.EnumSym, tparams: ResolvedAst.TypeParams, derives: List[Ast.Derivation], cases: List[ResolvedAst.Case], loc: SourceLocation) extends Declaration

  case class TypeAlias(doc: Ast.Doc, mod: Ast.Modifiers, sym: Symbol.TypeAliasSym, tparams: ResolvedAst.TypeParams, tpe: UnkindedType, loc: SourceLocation) extends Declaration

  case class Effect(doc: Ast.Doc, ann: List[ResolvedAst.Annotation], mod: Ast.Modifiers, sym: Symbol.EffectSym, ops: List[ResolvedAst.Op], loc: SourceLocation) extends Declaration

  case class Op(sym: Symbol.OpSym, spec: ResolvedAst.Spec) extends Declaration

  // TODO NS-REFACTOR make Declaration object
  sealed trait Declaration

  sealed trait Expression {
    def loc: SourceLocation
  }

  object Expression {

    case class Wild(loc: SourceLocation) extends ResolvedAst.Expression

    case class Var(sym: Symbol.VarSym, loc: SourceLocation) extends ResolvedAst.Expression

    case class Def(sym: Symbol.DefnSym, loc: SourceLocation) extends ResolvedAst.Expression

    case class Sig(sym: Symbol.SigSym, loc: SourceLocation) extends ResolvedAst.Expression

    case class Hole(sym: Symbol.HoleSym, loc: SourceLocation) extends ResolvedAst.Expression

    case class HoleWithExp(exp: ResolvedAst.Expression, loc: SourceLocation) extends ResolvedAst.Expression

    case class Use(sym: Symbol, exp: ResolvedAst.Expression, loc: SourceLocation) extends ResolvedAst.Expression

    case class Cst(cst: Ast.Constant, loc: SourceLocation) extends ResolvedAst.Expression

    case class Apply(exp: ResolvedAst.Expression, exps: List[ResolvedAst.Expression], loc: SourceLocation) extends ResolvedAst.Expression

    case class Lambda(fparam: ResolvedAst.FormalParam, exp: ResolvedAst.Expression, loc: SourceLocation) extends ResolvedAst.Expression

    case class Unary(sop: SemanticOperator, exp: ResolvedAst.Expression, loc: SourceLocation) extends ResolvedAst.Expression

    case class Binary(sop: SemanticOperator, exp1: ResolvedAst.Expression, exp2: ResolvedAst.Expression, loc: SourceLocation) extends ResolvedAst.Expression

    case class IfThenElse(exp1: ResolvedAst.Expression, exp2: ResolvedAst.Expression, exp3: ResolvedAst.Expression, loc: SourceLocation) extends ResolvedAst.Expression

    case class Stm(exp1: ResolvedAst.Expression, exp2: ResolvedAst.Expression, loc: SourceLocation) extends ResolvedAst.Expression

    case class Discard(exp: ResolvedAst.Expression, loc: SourceLocation) extends ResolvedAst.Expression

    case class Let(sym: Symbol.VarSym, mod: Ast.Modifiers, exp1: ResolvedAst.Expression, exp2: ResolvedAst.Expression, loc: SourceLocation) extends ResolvedAst.Expression

    case class LetRec(sym: Symbol.VarSym, mod: Ast.Modifiers, exp1: ResolvedAst.Expression, exp2: ResolvedAst.Expression, loc: SourceLocation) extends ResolvedAst.Expression

    // MATT why was this a full type
    case class Region(tpe: Type, loc: SourceLocation) extends ResolvedAst.Expression

    case class Scope(sym: Symbol.VarSym, regionVar: Symbol.UnkindedTypeVarSym, exp: ResolvedAst.Expression, loc: SourceLocation) extends ResolvedAst.Expression

    case class Match(exp: ResolvedAst.Expression, rules: List[ResolvedAst.MatchRule], loc: SourceLocation) extends ResolvedAst.Expression

    case class TypeMatch(exp: ResolvedAst.Expression, rules: List[ResolvedAst.MatchTypeRule], loc: SourceLocation) extends ResolvedAst.Expression

    case class Choose(star: Boolean, exps: List[ResolvedAst.Expression], rules: List[ResolvedAst.ChoiceRule], loc: SourceLocation) extends ResolvedAst.Expression

    case class Tag(sym: Ast.CaseSymUse, exp: ResolvedAst.Expression, loc: SourceLocation) extends ResolvedAst.Expression

    case class Tuple(elms: List[ResolvedAst.Expression], loc: SourceLocation) extends ResolvedAst.Expression

    case class RecordEmpty(loc: SourceLocation) extends ResolvedAst.Expression

    case class RecordSelect(exp: ResolvedAst.Expression, field: Name.Field, loc: SourceLocation) extends ResolvedAst.Expression

    case class RecordExtend(field: Name.Field, value: ResolvedAst.Expression, rest: ResolvedAst.Expression, loc: SourceLocation) extends ResolvedAst.Expression

    case class RecordRestrict(field: Name.Field, rest: ResolvedAst.Expression, loc: SourceLocation) extends ResolvedAst.Expression

    case class ArrayLit(exps: List[ResolvedAst.Expression], exp: ResolvedAst.Expression, loc: SourceLocation) extends ResolvedAst.Expression

    case class ArrayNew(exp1: ResolvedAst.Expression, exp2: ResolvedAst.Expression, exp3: ResolvedAst.Expression, loc: SourceLocation) extends ResolvedAst.Expression

    case class ArrayLoad(base: ResolvedAst.Expression, index: ResolvedAst.Expression, loc: SourceLocation) extends ResolvedAst.Expression

    case class ArrayStore(base: ResolvedAst.Expression, index: ResolvedAst.Expression, elm: ResolvedAst.Expression, loc: SourceLocation) extends ResolvedAst.Expression

    case class ArrayLength(base: ResolvedAst.Expression, loc: SourceLocation) extends ResolvedAst.Expression

    case class ArraySlice(base: ResolvedAst.Expression, beginIndex: ResolvedAst.Expression, endIndex: ResolvedAst.Expression, loc: SourceLocation) extends ResolvedAst.Expression

    case class Ref(exp1: ResolvedAst.Expression, exp2: ResolvedAst.Expression, loc: SourceLocation) extends ResolvedAst.Expression

    case class Deref(exp: ResolvedAst.Expression, loc: SourceLocation) extends ResolvedAst.Expression

    case class Assign(exp1: ResolvedAst.Expression, exp2: ResolvedAst.Expression, loc: SourceLocation) extends ResolvedAst.Expression

    case class Ascribe(exp: ResolvedAst.Expression, expectedType: Option[UnkindedType], expectedEff: UnkindedType.PurityAndEffect, loc: SourceLocation) extends ResolvedAst.Expression

    case class Cast(exp: ResolvedAst.Expression, declaredType: Option[UnkindedType], declaredEff: UnkindedType.PurityAndEffect, loc: SourceLocation) extends ResolvedAst.Expression

    case class Mask(exp: ResolvedAst.Expression, loc: SourceLocation) extends ResolvedAst.Expression

    case class Upcast(exp: ResolvedAst.Expression, loc: SourceLocation) extends ResolvedAst.Expression

    case class Supercast(exp: ResolvedAst.Expression, loc: SourceLocation) extends ResolvedAst.Expression

    case class Without(exp: ResolvedAst.Expression, eff: Ast.EffectSymUse, loc: SourceLocation) extends ResolvedAst.Expression

    case class TryCatch(exp: ResolvedAst.Expression, rules: List[ResolvedAst.CatchRule], loc: SourceLocation) extends ResolvedAst.Expression

    case class TryWith(exp: ResolvedAst.Expression, eff: Ast.EffectSymUse, rules: List[ResolvedAst.HandlerRule], loc: SourceLocation) extends ResolvedAst.Expression

    case class Do(op: Ast.OpSymUse, args: List[ResolvedAst.Expression], loc: SourceLocation) extends ResolvedAst.Expression

    case class Resume(exp: ResolvedAst.Expression, loc: SourceLocation) extends ResolvedAst.Expression

    case class InvokeConstructor(constructor: Constructor[_], args: List[ResolvedAst.Expression], loc: SourceLocation) extends ResolvedAst.Expression

    case class InvokeMethod(method: Method, clazz: java.lang.Class[_], exp: ResolvedAst.Expression, args: List[ResolvedAst.Expression], loc: SourceLocation) extends ResolvedAst.Expression

    case class InvokeStaticMethod(method: Method, args: List[ResolvedAst.Expression], loc: SourceLocation) extends ResolvedAst.Expression

    case class GetField(field: Field, clazz: java.lang.Class[_], exp: ResolvedAst.Expression, loc: SourceLocation) extends ResolvedAst.Expression

    case class PutField(field: Field, clazz: java.lang.Class[_], exp1: ResolvedAst.Expression, exp2: ResolvedAst.Expression, loc: SourceLocation) extends ResolvedAst.Expression

    case class GetStaticField(field: Field, loc: SourceLocation) extends ResolvedAst.Expression

    case class PutStaticField(field: Field, exp: ResolvedAst.Expression, loc: SourceLocation) extends ResolvedAst.Expression

    case class NewObject(name: String, clazz: java.lang.Class[_], methods: List[JvmMethod], loc: SourceLocation) extends ResolvedAst.Expression

    case class NewChannel(exp1: ResolvedAst.Expression, exp2: ResolvedAst.Expression, loc: SourceLocation) extends ResolvedAst.Expression

    case class GetChannel(exp: ResolvedAst.Expression, loc: SourceLocation) extends ResolvedAst.Expression

    case class PutChannel(exp1: ResolvedAst.Expression, exp2: ResolvedAst.Expression, loc: SourceLocation) extends ResolvedAst.Expression

    case class SelectChannel(rules: List[ResolvedAst.SelectChannelRule], default: Option[ResolvedAst.Expression], loc: SourceLocation) extends ResolvedAst.Expression

    case class Spawn(exp1: ResolvedAst.Expression, exp2: ResolvedAst.Expression, loc: SourceLocation) extends ResolvedAst.Expression

    case class Par(exp: Expression, loc: SourceLocation) extends ResolvedAst.Expression

    case class ParYield(frags: List[ResolvedAst.ParYieldFragment], exp: ResolvedAst.Expression, loc: SourceLocation) extends ResolvedAst.Expression

    case class Lazy(exp: ResolvedAst.Expression, loc: SourceLocation) extends ResolvedAst.Expression

    case class Force(exp: ResolvedAst.Expression, loc: SourceLocation) extends ResolvedAst.Expression

    case class FixpointConstraintSet(cs: List[ResolvedAst.Constraint], loc: SourceLocation) extends ResolvedAst.Expression

    case class FixpointLambda(pparams: List[ResolvedAst.PredicateParam], exp: ResolvedAst.Expression, loc: SourceLocation) extends ResolvedAst.Expression

    case class FixpointMerge(exp1: ResolvedAst.Expression, exp2: ResolvedAst.Expression, loc: SourceLocation) extends ResolvedAst.Expression

    case class FixpointSolve(exp: ResolvedAst.Expression, loc: SourceLocation) extends ResolvedAst.Expression

    case class FixpointFilter(pred: Name.Pred, exp: ResolvedAst.Expression, loc: SourceLocation) extends ResolvedAst.Expression

    case class FixpointInject(exp: ResolvedAst.Expression, pred: Name.Pred, loc: SourceLocation) extends ResolvedAst.Expression

    case class FixpointProject(pred: Name.Pred, exp1: ResolvedAst.Expression, exp2: ResolvedAst.Expression, loc: SourceLocation) extends ResolvedAst.Expression

  }

  sealed trait Pattern {
    def loc: SourceLocation
  }

  object Pattern {

    case class Wild(loc: SourceLocation) extends ResolvedAst.Pattern

    case class Var(sym: Symbol.VarSym, loc: SourceLocation) extends ResolvedAst.Pattern

    case class Cst(cst: Ast.Constant, loc: SourceLocation) extends ResolvedAst.Pattern

    case class Tag(sym: Ast.CaseSymUse, pat: ResolvedAst.Pattern, loc: SourceLocation) extends ResolvedAst.Pattern

    case class Tuple(elms: List[ResolvedAst.Pattern], loc: SourceLocation) extends ResolvedAst.Pattern

    case class Array(elms: List[ResolvedAst.Pattern], loc: SourceLocation) extends ResolvedAst.Pattern

    case class ArrayTailSpread(elms: scala.List[ResolvedAst.Pattern], sym: Symbol.VarSym, loc: SourceLocation) extends ResolvedAst.Pattern

    case class ArrayHeadSpread(sym: Symbol.VarSym, elms: scala.List[ResolvedAst.Pattern], loc: SourceLocation) extends ResolvedAst.Pattern

  }

  sealed trait ChoicePattern {
    def loc: SourceLocation
  }

  object ChoicePattern {

    case class Wild(loc: SourceLocation) extends ChoicePattern

    case class Absent(loc: SourceLocation) extends ChoicePattern

    case class Present(sym: Symbol.VarSym, loc: SourceLocation) extends ChoicePattern

  }

  sealed trait Predicate

  object Predicate {

    sealed trait Head extends ResolvedAst.Predicate

    object Head {

      case class Atom(pred: Name.Pred, den: Denotation, terms: List[ResolvedAst.Expression], loc: SourceLocation) extends ResolvedAst.Predicate.Head

    }

    sealed trait Body extends ResolvedAst.Predicate

    object Body {

      case class Atom(pred: Name.Pred, den: Denotation, polarity: Ast.Polarity, fixity: Ast.Fixity, terms: List[ResolvedAst.Pattern], loc: SourceLocation) extends ResolvedAst.Predicate.Body

      case class Guard(exp: ResolvedAst.Expression, loc: SourceLocation) extends ResolvedAst.Predicate.Body

      case class Loop(varSyms: List[Symbol.VarSym], exp: ResolvedAst.Expression, loc: SourceLocation) extends ResolvedAst.Predicate.Body

    }

  }

  sealed trait TypeParams {
    val tparams: List[ResolvedAst.TypeParam]
  }

  object TypeParams {

    case class Kinded(tparams: List[ResolvedAst.TypeParam.Kinded]) extends TypeParams

    case class Unkinded(tparams: List[ResolvedAst.TypeParam.Unkinded]) extends TypeParams

  }

  case class Annotation(name: Ast.Annotation, exps: List[ResolvedAst.Expression], loc: SourceLocation)

  case class Attribute(ident: Name.Ident, tpe: UnkindedType, loc: SourceLocation)

  case class Case(sym: Symbol.CaseSym, tpe: UnkindedType)

  case class Constraint(cparams: List[ResolvedAst.ConstraintParam], head: ResolvedAst.Predicate.Head, body: List[ResolvedAst.Predicate.Body], loc: SourceLocation)

  case class ConstraintParam(sym: Symbol.VarSym, loc: SourceLocation)

  case class FormalParam(sym: Symbol.VarSym, mod: Ast.Modifiers, tpe: Option[UnkindedType], loc: SourceLocation)

  sealed trait PredicateParam

  object PredicateParam {

    case class PredicateParamUntyped(pred: Name.Pred, loc: SourceLocation) extends PredicateParam

    case class PredicateParamWithType(pred: Name.Pred, den: Ast.Denotation, tpes: List[UnkindedType], loc: SourceLocation) extends PredicateParam

  }

  case class JvmMethod(ident: Name.Ident, fparams: Seq[ResolvedAst.FormalParam], exp: ResolvedAst.Expression, tpe: UnkindedType, purAndEff: UnkindedType.PurityAndEffect, loc: SourceLocation)

  case class CatchRule(sym: Symbol.VarSym, clazz: java.lang.Class[_], exp: ResolvedAst.Expression)

  case class HandlerRule(op: Ast.OpSymUse, fparams: Seq[ResolvedAst.FormalParam], exp: ResolvedAst.Expression)

  case class ChoiceRule(pat: List[ResolvedAst.ChoicePattern], exp: ResolvedAst.Expression)

  case class MatchRule(pat: ResolvedAst.Pattern, guard: Option[ResolvedAst.Expression], exp: ResolvedAst.Expression)

  case class MatchTypeRule(sym: Symbol.VarSym, tpe: UnkindedType, exp: ResolvedAst.Expression)

  case class SelectChannelRule(sym: Symbol.VarSym, chan: ResolvedAst.Expression, exp: ResolvedAst.Expression)

  sealed trait TypeParam {
    val name: Name.Ident
    val sym: Symbol.UnkindedTypeVarSym
  }

  object TypeParam {

    case class Kinded(name: Name.Ident, sym: Symbol.UnkindedTypeVarSym, kind: Kind, loc: SourceLocation) extends TypeParam

    case class Unkinded(name: Name.Ident, sym: Symbol.UnkindedTypeVarSym, loc: SourceLocation) extends TypeParam

  }

  case class TypeConstraint(head: Ast.TypeConstraint.Head, tpe: UnkindedType, loc: SourceLocation)

  case class ParYieldFragment(pat: ResolvedAst.Pattern, exp: Expression, loc: SourceLocation)

}
