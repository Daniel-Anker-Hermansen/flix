/*
 * Copyright 2015-2016 Ming-Ho Yee
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

package ca.uwaterloo.flix.language.phase

import ca.uwaterloo.flix.api.Flix
import ca.uwaterloo.flix.language.CompilationMessage
import ca.uwaterloo.flix.language.ast.{Ast, LiftedAst, SimplifiedAst, Symbol}
import ca.uwaterloo.flix.util.Validation._
import ca.uwaterloo.flix.util.{InternalCompilerException, Validation}

import scala.collection.mutable

object LambdaLift {

  /**
    * Mutable map of top level definitions.
    */
  private type TopLevel = mutable.Map[Symbol.DefnSym, LiftedAst.Def]

  /**
    * Performs lambda lifting on the given AST `root`.
    */
  def run(root: SimplifiedAst.Root)(implicit flix: Flix): Validation[LiftedAst.Root, CompilationMessage] = flix.phase("LambdaLift") {
    // A mutable map to hold lambdas that are lifted to the top level.
    val m: TopLevel = mutable.Map.empty

    val newDefs = root.defs.map {
      case (sym, decl) => sym -> liftDef(decl, m)
    }

    val newEnums = root.enums.map {
      case (sym, enum0) => sym -> visitEnum(enum0)
    }

    LiftedAst.Root(
      newDefs ++ m,
      newEnums,
      root.entryPoint,
      root.sources
    ).toSuccess
  }

  /**
    * Performs lambda lifting on the given definition `def0`.
    */
  private def liftDef(def0: SimplifiedAst.Def, m: TopLevel)(implicit flix: Flix): LiftedAst.Def = def0 match {
    case SimplifiedAst.Def(ann, mod, sym, fparams, exp, tpe, loc) =>
      val fs = fparams.map(visitFormalParam)
      val e = liftExp(def0.exp, sym, m)

      LiftedAst.Def(ann, mod, sym, fs, e, tpe, loc)
  }

  /**
    * Translates the given simplified enum declaration `enum0` into a lifted enum declaration.
    */
  private def visitEnum(enum0: SimplifiedAst.Enum): LiftedAst.Enum = enum0 match {
    case SimplifiedAst.Enum(ann, mod, sym, cases, tpeDeprecated, loc) =>
      val cs = cases.map {
        case (tag, SimplifiedAst.Case(caseSym, tpeDeprecated, loc)) => tag -> LiftedAst.Case(caseSym, tpeDeprecated, loc)
      }
      LiftedAst.Enum(ann, mod, sym, cs, tpeDeprecated, loc)
  }

  /**
    * Performs lambda lifting on the given expression `exp0` occurring with the given symbol `sym0`.
    */
  private def liftExp(exp0: SimplifiedAst.Expression, sym0: Symbol.DefnSym, m: TopLevel)(implicit flix: Flix): LiftedAst.Expression = {
    /**
      * Performs closure conversion and lambda lifting on the given expression `exp0`.
      */
    def visitExp(e: SimplifiedAst.Expression): LiftedAst.Expression = e match {
      case SimplifiedAst.Expression.Cst(cst, tpe, loc) => LiftedAst.Expression.Cst(cst, tpe, loc)

      case SimplifiedAst.Expression.Var(sym, tpe, loc) => LiftedAst.Expression.Var(sym, tpe, loc)

      case SimplifiedAst.Expression.LambdaClosure(fparams, freeVars, exp, tpe, loc) =>
        // Recursively lift the inner expression.
        val liftedExp = visitExp(exp)

        // Generate a fresh symbol for the new lifted definition.
        val freshSymbol = Symbol.freshDefnSym(sym0)

        // Construct annotations and modifiers for the fresh definition.
        val ann = Ast.Annotations.Empty
        val mod = Ast.Modifiers(Ast.Modifier.Synthetic :: Nil)

        // Construct the formal parameters.
        val fs = fparams.map(visitFormalParam)

        // Construct a new definition.
        val defn = LiftedAst.Def(ann, mod, freshSymbol, fs, liftedExp, tpe, loc)

        // Add the new definition to the map of lifted definitions.
        m += (freshSymbol -> defn)

        // Construct the closure args.
        val closureArgs = freeVars.map {
          case SimplifiedAst.FreeVar(sym, tpe) => LiftedAst.Expression.Var(sym, tpe, sym.loc)
        }

        // Construct the closure expression.
        LiftedAst.Expression.Closure(freshSymbol, closureArgs, tpe, loc)

      case SimplifiedAst.Expression.Closure(sym, tpe, loc) =>
        LiftedAst.Expression.Closure(sym, List.empty, tpe, loc)

      case SimplifiedAst.Expression.ApplyClo(exp, args, tpe, purity, loc) =>
        val e = visitExp(exp)
        val as = args map visitExp
        LiftedAst.Expression.ApplyClo(e, as, tpe, purity, loc)

      case SimplifiedAst.Expression.ApplyDef(sym, args, tpe, purity, loc) =>
        val as = args map visitExp
        LiftedAst.Expression.ApplyDef(sym, as, tpe, purity, loc)

      case SimplifiedAst.Expression.Unary(sop, op, exp, tpe, purity, loc) =>
        val e = visitExp(exp)
        LiftedAst.Expression.Unary(sop, op, e, tpe, purity, loc)

      case SimplifiedAst.Expression.Binary(sop, op, exp1, exp2, tpe, purity, loc) =>
        val e1 = visitExp(exp1)
        val e2 = visitExp(exp2)
        LiftedAst.Expression.Binary(sop, op, e1, e2, tpe, purity, loc)

      case SimplifiedAst.Expression.IfThenElse(exp1, exp2, exp3, tpe, purity, loc) =>
        val e1 = visitExp(exp1)
        val e2 = visitExp(exp2)
        val e3 = visitExp(exp3)
        LiftedAst.Expression.IfThenElse(e1, e2, e3, tpe, purity, loc)

      case SimplifiedAst.Expression.Branch(exp, branches, tpe, purity, loc) =>
        val e = visitExp(exp)
        val bs = branches map {
          case (sym, br) => sym -> visitExp(br)
        }
        LiftedAst.Expression.Branch(e, bs, tpe, purity, loc)

      case SimplifiedAst.Expression.JumpTo(sym, tpe, purity, loc) =>
        LiftedAst.Expression.JumpTo(sym, tpe, purity, loc)

      case SimplifiedAst.Expression.Let(sym, exp1, exp2, tpe, purity, loc) =>
        val e1 = visitExp(exp1)
        val e2 = visitExp(exp2)
        LiftedAst.Expression.Let(sym, e1, e2, tpe, purity, loc)

      case SimplifiedAst.Expression.LetRec(varSym, exp1, exp2, tpe, purity, loc) =>
        val e1 = visitExp(exp1)
        val e2 = visitExp(exp2)
        e1 match {
          case LiftedAst.Expression.Closure(defSym, closureArgs, _, _) =>
            val index = closureArgs.indexWhere {
              case LiftedAst.Expression.Var(sym, _, _) => varSym == sym
              case _ => false
            }
            if (index == -1) {
              // function never calls itself
              LiftedAst.Expression.Let(varSym, e1, e2, tpe, purity, loc)
            } else
              LiftedAst.Expression.LetRec(varSym, index, defSym, e1, e2, tpe, purity, loc)

          case _ => throw InternalCompilerException(s"Unexpected expression: '$e1'.", loc)
        }

      case SimplifiedAst.Expression.Region(tpe, loc) =>
        LiftedAst.Expression.Region(tpe, loc)

      case SimplifiedAst.Expression.Scope(sym, exp, tpe, purity, loc) =>
        val e = visitExp(exp)
        LiftedAst.Expression.Scope(sym, e, tpe, purity, loc)

      case SimplifiedAst.Expression.Is(sym, exp, purity, loc) =>
        val e = visitExp(exp)
        LiftedAst.Expression.Is(sym, e, purity, loc)

      case SimplifiedAst.Expression.Tag(sym, exp, tpe, purity, loc) =>
        val e = visitExp(exp)
        LiftedAst.Expression.Tag(sym, e, tpe, purity, loc)

      case SimplifiedAst.Expression.Untag(sym, exp, tpe, purity, loc) =>
        val e = visitExp(exp)
        LiftedAst.Expression.Untag(sym, e, tpe, purity, loc)

      case SimplifiedAst.Expression.Index(exp, offset, tpe, purity, loc) =>
        val e = visitExp(exp)
        LiftedAst.Expression.Index(e, offset, tpe, purity, loc)

      case SimplifiedAst.Expression.Tuple(elms, tpe, purity, loc) =>
        val es = elms map visitExp
        LiftedAst.Expression.Tuple(es, tpe, purity, loc)

      case SimplifiedAst.Expression.RecordEmpty(tpe, loc) =>
        LiftedAst.Expression.RecordEmpty(tpe, loc)

      case SimplifiedAst.Expression.RecordSelect(exp, field, tpe, purity, loc) =>
        val e = visitExp(exp)
        LiftedAst.Expression.RecordSelect(e, field, tpe, purity, loc)

      case SimplifiedAst.Expression.RecordExtend(field, value, rest, tpe, purity, loc) =>
        val v = visitExp(value)
        val r = visitExp(rest)
        LiftedAst.Expression.RecordExtend(field, v, r, tpe, purity, loc)

      case SimplifiedAst.Expression.RecordRestrict(field, rest, tpe, purity, loc) =>
        val r = visitExp(rest)
        LiftedAst.Expression.RecordRestrict(field, r, tpe, purity, loc)

      case SimplifiedAst.Expression.ArrayLit(elms, tpe, loc) =>
        val es = elms map visitExp
        LiftedAst.Expression.ArrayLit(es, tpe, loc)

      case SimplifiedAst.Expression.ArrayNew(elm, len, tpe, loc) =>
        val e = visitExp(elm)
        val l = visitExp(len)
        LiftedAst.Expression.ArrayNew(e, l, tpe, loc)

      case SimplifiedAst.Expression.ArrayLoad(base, index, tpe, loc) =>
        val b = visitExp(base)
        val i = visitExp(index)
        LiftedAst.Expression.ArrayLoad(b, i, tpe, loc)

      case SimplifiedAst.Expression.ArrayStore(base, index, elm, tpe, loc) =>
        val b = visitExp(base)
        val i = visitExp(index)
        val e = visitExp(elm)
        LiftedAst.Expression.ArrayStore(b, i, e, tpe, loc)

      case SimplifiedAst.Expression.ArrayLength(base, tpe, _, loc) =>
        val b = visitExp(base)
        val purity = b.purity
        LiftedAst.Expression.ArrayLength(b, tpe, purity, loc)

      case SimplifiedAst.Expression.ArraySlice(base, startIndex, endIndex, tpe, loc) =>
        val b = visitExp(base)
        val i1 = visitExp(startIndex)
        val i2 = visitExp(endIndex)
        LiftedAst.Expression.ArraySlice(b, i1, i2, tpe, loc)

      case SimplifiedAst.Expression.Ref(exp, tpe, loc) =>
        val e = visitExp(exp)
        LiftedAst.Expression.Ref(e, tpe, loc)

      case SimplifiedAst.Expression.Deref(exp, tpe, loc) =>
        val e = visitExp(exp)
        LiftedAst.Expression.Deref(e, tpe, loc)

      case SimplifiedAst.Expression.Assign(exp1, exp2, tpe, loc) =>
        val e1 = visitExp(exp1)
        val e2 = visitExp(exp2)
        LiftedAst.Expression.Assign(e1, e2, tpe, loc)

      case SimplifiedAst.Expression.Cast(exp, tpe, purity, loc) =>
        val e = visitExp(exp)
        LiftedAst.Expression.Cast(e, tpe, purity, loc)

      case SimplifiedAst.Expression.TryCatch(exp, rules, tpe, purity, loc) =>
        val e = visitExp(exp)
        val rs = rules map {
          case SimplifiedAst.CatchRule(sym, clazz, body) =>
            val b = visitExp(body)
            LiftedAst.CatchRule(sym, clazz, b)
        }
        LiftedAst.Expression.TryCatch(e, rs, tpe, purity, loc)

      case SimplifiedAst.Expression.InvokeConstructor(constructor, args, tpe, purity, loc) =>
        val as = args.map(visitExp)
        LiftedAst.Expression.InvokeConstructor(constructor, as, tpe, purity, loc)

      case SimplifiedAst.Expression.InvokeMethod(method, exp, args, tpe, purity, loc) =>
        val e = visitExp(exp)
        val as = args.map(visitExp)
        LiftedAst.Expression.InvokeMethod(method, e, as, tpe, purity, loc)

      case SimplifiedAst.Expression.InvokeStaticMethod(method, args, tpe, purity, loc) =>
        val as = args.map(visitExp)
        LiftedAst.Expression.InvokeStaticMethod(method, as, tpe, purity, loc)

      case SimplifiedAst.Expression.GetField(field, exp, tpe, purity, loc) =>
        val e = visitExp(exp)
        LiftedAst.Expression.GetField(field, e, tpe, purity, loc)

      case SimplifiedAst.Expression.PutField(field, exp1, exp2, tpe, purity, loc) =>
        val e1 = visitExp(exp1)
        val e2 = visitExp(exp2)
        LiftedAst.Expression.PutField(field, e1, e2, tpe, purity, loc)

      case SimplifiedAst.Expression.GetStaticField(field, tpe, purity, loc) =>
        LiftedAst.Expression.GetStaticField(field, tpe, purity, loc)

      case SimplifiedAst.Expression.PutStaticField(field, exp, tpe, purity, loc) =>
        val e = visitExp(exp)
        LiftedAst.Expression.PutStaticField(field, e, tpe, purity, loc)

      case SimplifiedAst.Expression.NewObject(name, clazz, tpe, purity, methods0, loc) =>
        val methods = methods0.map(visitJvmMethod)
        LiftedAst.Expression.NewObject(name, clazz, tpe, purity, methods, loc)

      case SimplifiedAst.Expression.Spawn(exp, tpe, loc) =>
        val e = visitExp(exp)
        LiftedAst.Expression.Spawn(e, tpe, loc)

      case SimplifiedAst.Expression.Lazy(exp, tpe, loc) =>
        val e = visitExp(exp)
        LiftedAst.Expression.Lazy(e, tpe, loc)

      case SimplifiedAst.Expression.Force(exp, tpe, loc) =>
        val e = visitExp(exp)
        LiftedAst.Expression.Force(e, tpe, loc)

      case SimplifiedAst.Expression.HoleError(sym, tpe, loc) =>
        LiftedAst.Expression.HoleError(sym, tpe, loc)

      case SimplifiedAst.Expression.MatchError(tpe, loc) =>
        LiftedAst.Expression.MatchError(tpe, loc)

      case SimplifiedAst.Expression.Def(_, _, loc) => throw InternalCompilerException(s"Unexpected expression.", loc)
      case SimplifiedAst.Expression.Lambda(_, _, _, loc) => throw InternalCompilerException(s"Unexpected expression.", loc)
      case SimplifiedAst.Expression.Apply(_, _, _, _, loc) => throw InternalCompilerException(s"Unexpected expression.", loc)
    }

    def visitJvmMethod(method: SimplifiedAst.JvmMethod): LiftedAst.JvmMethod = method match {
      case SimplifiedAst.JvmMethod(ident, fparams0, exp, retTpe, purity, loc) =>
        val fparams = fparams0 map visitFormalParam
        LiftedAst.JvmMethod(ident, fparams, visitExp(exp), retTpe, purity, loc)
    }

    visitExp(exp0)
  }

  /**
    * Translates the given simplified formal parameter `fparam` into a lifted formal parameter.
    */
  private def visitFormalParam(fparam: SimplifiedAst.FormalParam): LiftedAst.FormalParam = fparam match {
    case SimplifiedAst.FormalParam(sym, mod, tpe, loc) => LiftedAst.FormalParam(sym, mod, tpe, loc)
  }

}
