/*
 * Copyright 2021 Benjamin Dahse
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

namespace Fixpoint {

    use Fixpoint/Ram.{RamStmt, RelOp, RamSym, RamTerm, BoolExp, RowVar, toDenotation}
    use Fixpoint/Ast.Denotation
    use Fixpoint/Tuple.Tuple

    type alias Database[v: Type, r: Region] = MutMap[RamSym[v], MutMap[Tuple[v], v, r], r]
    type alias SearchEnv[v: Type, r: Region] = (Array[Tuple[v], r], Array[v, r])

    def interpret(r: Region[r], stmt: RamStmt[v]): Database[v, r] \ Write(r) with Order[v] =
        interpretWithDatabase(new MutMap(r), stmt)

    def interpretWithDatabase(db: Database[v, r], stmt: RamStmt[v]): Database[v, r] \ { Read(r), Write(r) } with Order[v] =
        unsafe_cast notifyPreInterpret(stmt) as _ \ r; // This casts away the impure effect of notify
        evalStmt(db, stmt);
        db

    def evalStmt(db: Database[v, r], stmt: RamStmt[v]): Unit \ { Read(r), Write(r) } with Order[v] =
        let r = Scoped.regionOf(db);
        match stmt {
            case RamStmt.Insert(relOp) => evalOp(db, allocEnv(r, 0, relOp), relOp)
            case RamStmt.Merge(srcSym, dstSym) =>
                let dst = MutMap.getOrElsePut!(dstSym, new MutMap(r), db);
                match toDenotation(srcSym) {
                    case Denotation.Relational =>
                        MutMap.merge!(MutMap.getWithDefault(srcSym, new MutMap(r), db), dst)
                    case Denotation.Latticenal(_, _, lub, _) =>
                        MutMap.mergeWith!(lub, MutMap.getWithDefault(srcSym, new MutMap(r), db), dst)
                }
            case RamStmt.Assign(lhs, rhs) =>
                MutMap.put!(lhs, MutMap.getWithDefault(rhs, new MutMap(r), db), db)
            case RamStmt.Purge(ramSym) => MutMap.remove!(ramSym, db)
            case RamStmt.Seq(stmts) => List.forEach(evalStmt(db), stmts)
            case RamStmt.Until(test, body) =>
                if (evalBoolExp(db, ([] @ r, [] @ r), test)) {
                    ()
                } else {
                    evalStmt(db, body);
                    evalStmt(db, stmt)
                }
            case RamStmt.Comment(_) => ()
        }

    def allocEnv(r: Region[r], depth: Int32, relOp: RelOp[v]): SearchEnv[v, r] \ Write(r) = match relOp {
        case RelOp.Search(_, _, body) => allocEnv(r, depth + 1, body)
        case RelOp.Query(_, _, _, body) => allocEnv(r, depth + 1, body)
        case RelOp.Project(_) => (Array.new(r, Fixpoint/Tuple.new(Nil), depth), Array.new(r, Reflect.default(), depth))
        case RelOp.If(_, then) => allocEnv(r, depth, then)
    }

    def evalOp(db: Database[v, r1], env: SearchEnv[v, r2], op: RelOp[v]): Unit \ { Read(r1), Write(r1), Read(r2), Write(r2) } with Order[v] =
        let r1 = Scoped.regionOf(db);
        match op {
            case RelOp.Search(RowVar.Index(i), ramSym, body) =>
                let (tupleEnv, latEnv) = env;
                MutMap.forEach(t -> l -> {
                    tupleEnv[i] = t;
                    latEnv[i] = l;
                    evalOp(db, env, body)
                }, MutMap.getWithDefault(ramSym, new MutMap(r1), db))
            case RelOp.Query(RowVar.Index(i), ramSym, query, body) =>
                let (tupleEnv, latEnv) = env;
                MutMap.queryWith(evalQuery(env, query), t -> l -> {
                    tupleEnv[i] = t;
                    latEnv[i] = l;
                    evalOp(db, env, body)
                }, MutMap.getWithDefault(ramSym, new MutMap(r1), db))
            case RelOp.Project(terms, ramSym) =>
                let rel = MutMap.getOrElsePut!(ramSym, new MutMap(r1), db);
                match toDenotation(ramSym) {
                    case Denotation.Relational =>
                        let tuple = Fixpoint/Tuple.new(List.map(evalTerm(env), terms));
                        MutMap.put!(tuple, Reflect.default(), rel)
                    case Denotation.Latticenal(bot, leq, lub, _) =>
                        // assume that length(terms) > 0
                        let len = List.length(terms);
                        let keyList = terms |> List.map(evalTerm(env));
                        let (relKeys, latValList) = List.splitAt(len-1, keyList);
                        let key = Fixpoint/Tuple.new(relKeys);
                        let latVal = match List.head(latValList) {
                            case None => bug!("Found predicate without terms")
                            case Some(k) => k
                        };
                        if (latVal `leq` bot) ()
                        else MutMap.putWith!(lub, key, latVal, rel)
                }
            case RelOp.If(test, then) =>
                if (evalBoolExp(db, env, test)) {
                    evalOp(db, env, then)
                } else {
                    ()
                }
            case _ => ()
        }

    def evalQuery(env: SearchEnv[v, r], query: List[(Int32, RamTerm[v])], tuple: Tuple[v]): Comparison \ Read(r) with Order[v] =
        match query {
            case Nil => EqualTo
            case (j, t) :: tl => match Fixpoint/Tuple.valueAt(tuple, j) <=> evalTerm(env, t) {
                case EqualTo => evalQuery(env, tl, tuple)
                case cmp => cmp
            }
        }

    def evalBoolExp(db: Database[v, r1], env: SearchEnv[v, r2], es: List[BoolExp[v]]): Bool \ { Read(r1), Read(r2) } with Order[v] =
        let r1 = Scoped.regionOf(db);
        List.forAll(exp -> match exp {
            case BoolExp.Empty(ramSym) =>
                MutMap.isEmpty(MutMap.getWithDefault(ramSym, new MutMap(r1), db))
            case BoolExp.NotMemberOf(terms, ramSym) =>
                let rel = MutMap.getWithDefault(ramSym, new MutMap(r1), db);
                match toDenotation(ramSym) {
                    case Denotation.Relational =>
                        let tuple = Fixpoint/Tuple.new(List.map(evalTerm(env), terms));
                        not MutMap.memberOf(tuple, rel)
                    case Denotation.Latticenal(bot, leq, _, _) =>
                        let len = List.length(terms);
                        let (keyTerms, latTermList) = terms |> List.map(evalTerm(env)) |> List.splitAt(len - 1);
                        let key = Fixpoint/Tuple.new(keyTerms);
                        let latTerm = match List.head(latTermList) {
                            case None => bug!("Found predicate without terms")
                            case Some(hd) => hd
                        };
                        not (latTerm `leq` MutMap.getWithDefault(key, bot, rel))
                }
            case BoolExp.Eq(lhs, rhs) =>
                evalTerm(env, lhs) == evalTerm(env, rhs)
            case BoolExp.Leq(leq, lhs, rhs) =>
                evalTerm(env, lhs) `leq` evalTerm(env, rhs)
            case BoolExp.Guard0(f) =>
                f()
            case BoolExp.Guard1(f, t) =>
                let v = evalTerm(env, t);
                f(v)
            case BoolExp.Guard2(f, t1, t2) =>
                let v1 = evalTerm(env, t1);
                let v2 = evalTerm(env, t2);
                f(v1)(v2)
            case BoolExp.Guard3(f, t1, t2, t3) =>
                let v1 = evalTerm(env, t1);
                let v2 = evalTerm(env, t2);
                let v3 = evalTerm(env, t3);
                f(v1)(v2)(v3)
            case BoolExp.Guard4(f, t1, t2, t3, t4) =>
                let v1 = evalTerm(env, t1);
                let v2 = evalTerm(env, t2);
                let v3 = evalTerm(env, t3);
                let v4 = evalTerm(env, t4);
                f(v1)(v2)(v3)(v4)
            case BoolExp.Guard5(f, t1, t2, t3, t4, t5) =>
                let v1 = evalTerm(env, t1);
                let v2 = evalTerm(env, t2);
                let v3 = evalTerm(env, t3);
                let v4 = evalTerm(env, t4);
                let v5 = evalTerm(env, t5);
                f(v1)(v2)(v3)(v4)(v5)
        }, es)

    def evalTerm(env: SearchEnv[v, r], term: RamTerm[v]): v \ Read(r) = match term {
        case RamTerm.Lit(v) => v
        case RamTerm.RowLoad(RowVar.Index(i), index) =>
            let (tupleEnv, _) = env;
            Fixpoint/Tuple.valueAt(tupleEnv[i], index)
        case RamTerm.LoadLatVar(RowVar.Index(i)) =>
            let (_, latEnv) = env;
            latEnv[i]
        case RamTerm.Meet(cap, lhs, rhs) =>
            evalTerm(env, lhs) `cap` evalTerm(env, rhs)
        case RamTerm.App0(f) =>
            f()
        case RamTerm.App1(f, t) =>
            let v = evalTerm(env, t);
            f(v)
        case RamTerm.App2(f, t1, t2) =>
            let v1 = evalTerm(env, t1);
            let v2 = evalTerm(env, t2);
            f(v1)(v2)
        case RamTerm.App3(f, t1, t2, t3) =>
            let v1 = evalTerm(env, t1);
            let v2 = evalTerm(env, t2);
            let v3 = evalTerm(env, t3);
            f(v1)(v2)(v3)
        case RamTerm.App4(f, t1, t2, t3, t4) =>
            let v1 = evalTerm(env, t1);
            let v2 = evalTerm(env, t2);
            let v3 = evalTerm(env, t3);
            let v4 = evalTerm(env, t4);
            f(v1)(v2)(v3)(v4)
        case RamTerm.App5(f, t1, t2, t3, t4, t5) =>
            let v1 = evalTerm(env, t1);
            let v2 = evalTerm(env, t2);
            let v3 = evalTerm(env, t3);
            let v4 = evalTerm(env, t4);
            let v5 = evalTerm(env, t5);
            f(v1)(v2)(v3)(v4)(v5)
        case _ => bug!("Illegal term ${term}")
    }
}
