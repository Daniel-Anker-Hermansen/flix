/*
 * Copyright 2017 Magnus Madsen
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
import ca.uwaterloo.flix.language.ast.LiftedAst.Root
import ca.uwaterloo.flix.language.ast.LiftedAst._
import ca.uwaterloo.flix.language.ast.Symbol
import ca.uwaterloo.flix.util.{ParOps, Validation}
import ca.uwaterloo.flix.util.Validation._

/**
  * The Tree Shaking phase removes all unused function definitions.
  *
  * A function is considered reachable if it:
  *
  * (a) The main function is always reachable.
  * (b) A function marked with @benchmark or @test is reachable.
  * (c) Appears in a function which itself is reachable.
  *
  */
object LateTreeShaker {

  /**
    * Performs tree shaking on the given AST `root`.
    */
  def run(root: Root)(implicit flix: Flix): Validation[Root, CompilationMessage] = flix.phase("LateTreeShaker") {
    // Compute the symbols that are always reachable.
    val initReach = initReachable(root)

    // Compute the symbols that are transitively reachable.
    val allReachable = ParOps.parReachable(initReach, visitSym(_, root))

    // Filter the reachable definitions.
    val newDefs = root.defs.filter {
      case (sym, _) => allReachable.contains(sym)
    }

    // Reassemble the AST.
    root.copy(defs = newDefs).toSuccess
  }

  /**
    * Returns the symbols that are always reachable.
    */
  private def initReachable(root: Root): Set[Symbol.DefnSym] = {
    // A set used to collect the symbols of reachable functions.
    var reachable: Set[Symbol.DefnSym] = Set.empty

    //
    // (a) The main function is always reachable (if it exists).
    //
    reachable = reachable ++ root.entryPoint

    //
    // (b) A function annotated with @benchmark or @test is always reachable.
    //
    for ((sym, defn) <- root.defs) {
      val isBenchmark = defn.ann.isBenchmark
      val isTest = defn.ann.isTest
      if (isBenchmark || isTest) {
        reachable = reachable + sym
      }
    }

    reachable
  }

  /**
    * Returns the symbols reachable from the given symbol `sym`.
    */
  private def visitSym(sym: Symbol.DefnSym, root: Root): Set[Symbol.DefnSym] = root.defs.get(sym) match {
    case None => Set.empty
    case Some(defn) => visitExp(defn.exp)
  }

  /**
    * Returns the function symbols reachable from the given expression `e0`.
    */
  private def visitExp(e0: Expression): Set[Symbol.DefnSym] = e0 match {
    case Expression.Cst(_, _, _) =>
      Set.empty

    case Expression.Var(_, _, _) =>
      Set.empty

    case Expression.Closure(sym, closureArgs, _, _) =>
      Set(sym) ++ visitExps(closureArgs)

    case Expression.ApplyClo(exp, args, _, _, _) =>
      visitExp(exp) ++ visitExps(args)

    case Expression.ApplyDef(sym, args, _, _, _) =>
      Set(sym) ++ visitExps(args)

    case Expression.ApplyCloTail(exp, args, _, _, _) =>
      visitExp(exp) ++ visitExps(args)

    case Expression.ApplyDefTail(sym, args, _, _, _) =>
      Set(sym) ++ visitExps(args)

    case Expression.ApplySelfTail(sym, _, args, _, _, _) =>
      Set(sym) ++ visitExps(args)

    case Expression.Unary(_, _, exp, _, _, _) =>
      visitExp(exp)

    case Expression.Binary(_, _, exp1, exp2, _, _, _) =>
      visitExp(exp1) ++ visitExp(exp2)

    case Expression.IfThenElse(exp1, exp2, exp3, _, _, _) =>
      visitExp(exp1) ++ visitExp(exp2) ++ visitExp(exp3)

    case Expression.Branch(exp, branches, _, _, _) =>
      visitExp(exp) ++ visitExps(branches.values.toList)

    case Expression.JumpTo(_, _, _, _) =>
      Set.empty

    case Expression.Let(_, exp1, exp2, _, _, _) =>
      visitExp(exp1) ++ visitExp(exp2)

    case Expression.LetRec(_, _, _, exp1, exp2, _, _, _) =>
      visitExp(exp1) ++ visitExp(exp2)

    case Expression.Region(_, _) =>
      Set.empty

    case Expression.Scope(_, exp, _, _, _) =>
      visitExp(exp)

    case Expression.Is(_, exp, _, _) =>
      visitExp(exp)

    case Expression.Tag(_, exp, _, _, _) =>
      visitExp(exp)

    case Expression.Untag(_, exp, _, _, _) =>
      visitExp(exp)

    case Expression.Index(exp, _, _, _, _) =>
      visitExp(exp)

    case Expression.Tuple(elms, _, _, _) =>
      visitExps(elms)

    case Expression.RecordEmpty(_, _) =>
      Set.empty

    case Expression.RecordSelect(exp, _, _, _, _) =>
      visitExp(exp)

    case Expression.RecordExtend(_, value, rest, _, _, _) =>
      visitExp(value) ++ visitExp(rest)

    case Expression.RecordRestrict(_, rest, _, _, _) =>
      visitExp(rest)

    case Expression.ArrayLit(elms, _, _) =>
      visitExps(elms)

    case Expression.ArrayNew(elm, len, _, _) =>
      visitExp(elm) ++ visitExp(len)

    case Expression.ArrayLoad(base, index, _, _) =>
      visitExp(base) ++ visitExp(index)

    case Expression.ArrayStore(base, index, elm, _, _) =>
      visitExp(base) ++ visitExp(index) ++ visitExp(elm)

    case Expression.ArrayLength(base, _, _, _) =>
      visitExp(base)

    case Expression.ArraySlice(base, startIndex, endIndex, _, _) =>
      visitExp(base) ++ visitExp(startIndex) ++ visitExp(endIndex)

    case Expression.Ref(exp, _, _) =>
      visitExp(exp)

    case Expression.Deref(exp, _, _) =>
      visitExp(exp)

    case Expression.Assign(exp1, exp2, _, _) =>
      visitExp(exp1) ++ visitExp(exp2)

    case Expression.Cast(exp, _, _, _) =>
      visitExp(exp)

    case Expression.TryCatch(exp, rules, _, _, _) =>
      visitExp(exp) ++ visitExps(rules.map(_.exp))

    case Expression.InvokeConstructor(_, args, _, _, _) =>
      visitExps(args)

    case Expression.InvokeMethod(_, exp, args, _, _, _) =>
      visitExp(exp) ++ visitExps(args)

    case Expression.InvokeStaticMethod(_, args, _, _, _) =>
      visitExps(args)

    case Expression.GetField(_, exp, _, _, _) =>
      visitExp(exp)

    case Expression.PutField(_, exp1, exp2, _, _, _) =>
      visitExp(exp1) ++ visitExp(exp2)

    case Expression.GetStaticField(_, _, _, _) =>
      Set.empty

    case Expression.PutStaticField(_, exp, _, _, _) =>
      visitExp(exp)

    case Expression.NewObject(_, _, _, _, methods, _) =>
      visitExps(methods.map(_.clo))

    case Expression.Spawn(exp, _, _) =>
      visitExp(exp)

    case Expression.Lazy(exp, _, _) =>
      visitExp(exp)

    case Expression.Force(exp, _, _) =>
      visitExp(exp)

    case Expression.HoleError(_, _, _) =>
      Set.empty

    case Expression.MatchError(_, _) =>
      Set.empty

  }

  /**
    * Returns the function symbols reachable from `es`.
    */
  private def visitExps(es: List[Expression]): Set[Symbol.DefnSym] = es.map(visitExp).fold(Set())(_ ++ _)

}
