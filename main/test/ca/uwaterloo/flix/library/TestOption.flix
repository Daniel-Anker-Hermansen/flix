/*
 * Copyright 2017 Liam Palmer
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

namespace TestOption {

    use Hash.hash

/////////////////////////////////////////////////////////////////////////////
// isEmpty                                                                 //
/////////////////////////////////////////////////////////////////////////////
@test
def isEmpty01(): Bool = Option.isEmpty(None) == true

@test
def isEmpty02(): Bool = Option.isEmpty(Some(0)) == false

@test
def isEmpty03(): Bool = Option.isEmpty(Some(22ii)) == false

/////////////////////////////////////////////////////////////////////////////
// getWithDefault                                                          //
/////////////////////////////////////////////////////////////////////////////
@test
def getWithDefault01(): Bool = Option.getWithDefault(32, None) == 32

@test
def getWithDefault02(): Bool = Option.getWithDefault(32, Some(30)) == 30

@test
def getWithDefault03(): Bool = Option.getWithDefault(32ii, None) == 32ii

@test
def getWithDefault04(): Bool = Option.getWithDefault(32ii, Some(30ii)) == 30ii

/////////////////////////////////////////////////////////////////////////////
// exists                                                                  //
/////////////////////////////////////////////////////////////////////////////
@test
def exists01(): Bool = Option.exists(i -> i == 2, None) == false

@test
def exists02(): Bool = Option.exists(i -> i == 2, Some(1)) == false

@test
def exists03(): Bool = Option.exists(i -> i == 2, Some(2)) == true

/////////////////////////////////////////////////////////////////////////////
// forAll                                                                  //
/////////////////////////////////////////////////////////////////////////////
@test
def forAll01(): Bool = Option.forAll(i -> i == 2, None) == true

@test
def forAll02(): Bool = Option.forAll(i -> i == 2, Some(1)) == false

@test
def forAll03(): Bool = Option.forAll(i -> i == 2, Some(2)) == true

/////////////////////////////////////////////////////////////////////////////
// filter                                                                  //
/////////////////////////////////////////////////////////////////////////////
@test
def filter01(): Bool = Option.filter(i -> i == 2, None) == None

@test
def filter02(): Bool = Option.filter(i -> i == 2, Some(1)) == None

@test
def filter03(): Bool = Option.filter(i -> i == 2, Some(2)) == Some(2)

/////////////////////////////////////////////////////////////////////////////
// map                                                                     //
/////////////////////////////////////////////////////////////////////////////
@test
def map01(): Bool = Option.map(i -> 2*i, None) == None

@test
def map02(): Bool = Option.map(i -> 2*i, Some(1)) == Some(2)

@test
def map03(): Bool = Option.map(i -> 2*i, Some(2)) == Some(4)

@test
def map04(): Bool \ IO =
    let n = ref 2;
    discard Option.map(i -> n := i, Some(4));
    4 == deref n

/////////////////////////////////////////////////////////////////////////////
// toList                                                                  //
/////////////////////////////////////////////////////////////////////////////
@test
def toList01(): Bool = Option.toList(None: Option[Unit]) == Nil

@test
def toList02(): Bool = Option.toList(Some(1)) == 1 :: Nil

/////////////////////////////////////////////////////////////////////////////
// toSet                                                                   //
/////////////////////////////////////////////////////////////////////////////
@test
def toSet01(): Bool = Option.toSet(None: Option[Unit]) == Set#{}

@test
def toSet02(): Bool = Option.toSet(Some(1)) == Set#{1}

/////////////////////////////////////////////////////////////////////////////
// toMap                                                                   //
/////////////////////////////////////////////////////////////////////////////
@test
def toMap01(): Bool = Option.toMap(None: Option[(Unit, Unit)]) == Map#{}

@test
def toMap02(): Bool = Option.toMap(Some((1, true))) == Map#{1 => true}

/////////////////////////////////////////////////////////////////////////////
// toMapWith                                                               //
/////////////////////////////////////////////////////////////////////////////

@test
def toMapWith01(): Bool =
    Option.toMapWith(x -> 0 * x, None) == Map#{}

@test
def toMapWith02(): Bool =
    Option.toMapWith(x -> 0 * x, Some(1)) == Map#{1 => 0}

@test
def toMapWith03(): Bool =
    Option.toMapWith(x -> 2 * x, Some(1)) == Map#{1 => 2}

@test
def toMapWith04(): Bool =
    Option.toMapWith(x -> x + 10, Some(1)) == Map#{1 => 11}

@test
def toMapWith05(): Bool =
    Option.toMapWith(x -> x + "b", Some("a")) == Map#{"a" => "ab"}

/////////////////////////////////////////////////////////////////////////////
// toOk                                                                   //
/////////////////////////////////////////////////////////////////////////////
@test
def toOk01(): Bool = Option.toOk("err", None: Option[Unit]) == Err("err")

@test
def toOk02(): Bool = Option.toOk("err", Some(1)) == Ok(1)

/////////////////////////////////////////////////////////////////////////////
// toErr                                                                   //
/////////////////////////////////////////////////////////////////////////////
@test
def toErr01(): Bool = Option.toErr("default", None: Option[Unit]) == Ok("default")

@test
def toErr02(): Bool = Option.toErr("default", Some("err")) == Err("err")

/////////////////////////////////////////////////////////////////////////////
// toSuccess                                                               //
/////////////////////////////////////////////////////////////////////////////
@test
def toSuccess01(): Bool = Option.toSuccess("err", None: Option[Unit]) == Failure(Nec.singleton("err"))

@test
def toSuccess02(): Bool = Option.toSuccess("err", Some(1)) == Success(1)

/////////////////////////////////////////////////////////////////////////////
// toFailure                                                               //
/////////////////////////////////////////////////////////////////////////////
@test
def toFailure01(): Bool = Option.toFailure("default", None: Option[Unit]) == Success("default")

@test
def toFailure02(): Bool = Option.toFailure("default", Some("err")) == Failure(Nec.singleton("err"))

/////////////////////////////////////////////////////////////////////////////
// withDefault                                                             //
/////////////////////////////////////////////////////////////////////////////
@test
def withDefault01(): Bool = Option.withDefault(default = None, None) == None: Option[Int32]

@test
def withDefault02(): Bool = Option.withDefault(default = Some(1), None) == Some(1)

@test
def withDefault03(): Bool = Option.withDefault(default = None, Some(1)) == Some(1)

@test
def withDefault04(): Bool = Option.withDefault(default = Some(2), Some(1)) == Some(1)

/////////////////////////////////////////////////////////////////////////////
// replace                                                                 //
/////////////////////////////////////////////////////////////////////////////
@test
def replace01(): Bool = Option.replace(from = 3, to = 4, None) == None

@test
def replace02(): Bool = Option.replace(from = 3, to = 4, Some(2)) == Some(2)

@test
def replace03(): Bool = Option.replace(from = 3, to = 4, Some(3)) == Some(4)

@test
def replace04(): Bool = Option.replace(from = 3, to = 4, Some(4)) == Some(4)

/////////////////////////////////////////////////////////////////////////////
// count                                                                   //
/////////////////////////////////////////////////////////////////////////////
@test
def count01(): Bool = Option.count(i -> i == 2, None) == 0

@test
def count02(): Bool = Option.count(i -> i == 2, Some(1)) == 0

@test
def count03(): Bool = Option.count(i -> i == 2, Some(2)) == 1


    /////////////////////////////////////////////////////////////////////////////
    // sum                                                                     //
    /////////////////////////////////////////////////////////////////////////////

    @test
    def sum01(): Bool = Option.sum(None) == 0

    @test
    def sum02(): Bool = Option.sum(Some(1)) == 1

    @test
    def sum03(): Bool = Option.sum(Some(-2)) == -2


    /////////////////////////////////////////////////////////////////////////////
    // sumWith                                                                 //
    /////////////////////////////////////////////////////////////////////////////

    @test
    def sumWith01(): Bool = Option.sumWith(x -> x + 1, None) == 0

    @test
    def sumWith02(): Bool = Option.sumWith(x -> x + 1, Some(1)) == 2

    @test
    def sumWith03(): Bool = Option.sumWith(x -> x + 1, Some(-2)) == -1


    /////////////////////////////////////////////////////////////////////////////
    // product                                                                 //
    /////////////////////////////////////////////////////////////////////////////

    @test
    def product01(): Bool = Option.product(None) == 1

    @test
    def product02(): Bool = Option.product(Some(1)) == 1

    @test
    def product03(): Bool = Option.product(Some(-2)) == -2


    /////////////////////////////////////////////////////////////////////////////
    // productWith                                                             //
    /////////////////////////////////////////////////////////////////////////////

    @test
    def productWith01(): Bool = Option.productWith(x -> x + 1, None) == 1

    @test
    def productWith02(): Bool = Option.productWith(x -> x + 1, Some(1)) == 2

    @test
    def productWith03(): Bool = Option.productWith(x -> x + 1, Some(-2)) == -1


/////////////////////////////////////////////////////////////////////////////
// find                                                                    //
/////////////////////////////////////////////////////////////////////////////
@test
def find01(): Bool = Option.find(i -> i == 2, None) == None

@test
def find02(): Bool = Option.find(i -> i == 2, Some(1)) == None

@test
def find03(): Bool = Option.find(i -> i == 2, Some(2)) == Some(2)

/////////////////////////////////////////////////////////////////////////////
// flatten                                                                 //
/////////////////////////////////////////////////////////////////////////////
@test
def flatten01(): Bool = Option.flatten(None): Option[Unit] == None

@test
def flatten02(): Bool = Option.flatten(Some(None: Option[Unit])) == None

@test
def flatten03(): Bool = Option.flatten(Some(Some(2))) == Some(2)

    /////////////////////////////////////////////////////////////////////////////
    // fold                                                                    //
    /////////////////////////////////////////////////////////////////////////////

    @test
    def fold01(): Bool = Option.fold(None: Option[Unit]) == ()

    @test
    def fold02(): Bool = Option.fold(Some("a")) == "a"

    @test
    def fold03(): Bool = Option.fold(Some(())) == ()

    @test
    def fold04(): Bool = Option.fold(Some(("a", "b", "c"))) == ("a", "b", "c")

/////////////////////////////////////////////////////////////////////////////
// foldLeft                                                                //
/////////////////////////////////////////////////////////////////////////////
@test
def foldLeft01(): Bool = Option.foldLeft((b, i) -> if (i == 2 and b) true else false, false, None) == false

@test
def foldLeft02(): Bool = Option.foldLeft((b, i) -> if (i == 2 and b) true else false, false, Some(1)) == false

@test
def foldLeft03(): Bool = Option.foldLeft((b, i) -> if (i == 2 and b) true else false, false, Some(2)) == false

@test
def foldLeft04(): Bool = Option.foldLeft((b, i) -> if (i == 2 and b) true else false, true, Some(1)) == false

@test
def foldLeft05(): Bool = Option.foldLeft((b, i) -> if (i == 2 and b) true else false, true, Some(2)) == true

/////////////////////////////////////////////////////////////////////////////
// foldRight                                                               //
/////////////////////////////////////////////////////////////////////////////
@test
def foldRight01(): Bool = Option.foldRight((i, acc) -> if (i == 2 and acc) true else false, None, false) == false

@test
def foldRight02(): Bool = Option.foldRight((i, acc) -> if (i == 2 and acc) true else false, Some(1), false) == false

@test
def foldRight03(): Bool = Option.foldRight((i, acc) -> if (i == 2 and acc) true else false, Some(2), false) == false

@test
def foldRight04(): Bool = Option.foldRight((i, acc) -> if (i == 2 and acc) true else false, Some(1), true) == false

@test
def foldRight05(): Bool = Option.foldRight((i, acc) -> if (i == 2 and acc) true else false, Some(2), true) == true

/////////////////////////////////////////////////////////////////////////////
// foldRightWithCont                                                       //
/////////////////////////////////////////////////////////////////////////////
@test
def foldRightWithCont01(): Bool = Option.foldRightWithCont((i, k) -> if (i == 2 and k()) true else false, None, false) == false

@test
def foldRightWithCont02(): Bool = Option.foldRightWithCont((i, k) -> if (i == 2 and k()) true else false, Some(1), false) == false

@test
def foldRightWithCont03(): Bool = Option.foldRightWithCont((i, k) -> if (i == 2 and k()) true else false, Some(2), false) == false

@test
def foldRightWithCont04(): Bool = Option.foldRightWithCont((i, k) -> if (i == 2 and k()) true else false, Some(1), true) == false

@test
def foldRightWithCont05(): Bool = Option.foldRightWithCont((i, k) -> if (i == 2 and k()) true else false, Some(2), true) == true

/////////////////////////////////////////////////////////////////////////////
// sequence                                                                //
/////////////////////////////////////////////////////////////////////////////
@test
def sequence01(): Bool = Option.sequence(Nil): Option[List[Unit]] == Some(Nil)

@test
def sequence02(): Bool = Option.sequence(Some(1) :: Nil) == Some(1 :: Nil)

@test
def sequence03(): Bool = Option.sequence(Some(1) :: Some(2) :: Nil) == Some(1 :: 2 :: Nil)

@test
def sequence04(): Bool = Option.sequence(Some(1) :: Some(2) :: Some(3) :: Nil) == Some(1 :: 2 :: 3 :: Nil)

@test
def sequence05(): Bool = Option.sequence(None :: Some(2) :: Some(3) :: Nil) == None

@test
def sequence06(): Bool = Option.sequence(Some(1) :: Some(2) :: None :: Nil) == None

@test
def sequence07(): Bool = Option.sequence(Some(1) :: None :: Some(3) :: Nil) == None

/////////////////////////////////////////////////////////////////////////////
// traverse                                                                //
/////////////////////////////////////////////////////////////////////////////
@test
def traverse01(): Bool = Option.traverse(x -> Some(x + 1), Nil) == Some(Nil)

@test
def traverse02(): Bool = Option.traverse(x -> Some(x + 1), 1 :: Nil) == Some(2 :: Nil)

@test
def traverse03(): Bool = Option.traverse(x -> Some(x + 1), 1 :: 2 :: Nil) == Some(2 :: 3 :: Nil)

@test
def traverse04(): Bool = Option.traverse(x -> Some(x + 1), 1 :: 2 :: 3 :: Nil) == Some(2 :: 3 :: 4 :: Nil)

@test
def traverse05(): Bool = Option.traverse(x -> if (x == 1) None else Some(x), 1 :: 2 :: 3 :: Nil) == None

@test
def traverse06(): Bool = Option.traverse(x -> if (x == 3) None else Some(x), 1 :: 2 :: 3 :: Nil) == None

@test
def traverse07(): Bool = Option.traverse(x -> if (x == 2) None else Some(x), 1 :: 2 :: 3 :: Nil) == None

@test
def traverse08(): Bool = Option.traverse(_ -> Some(42), 1 :: 2 :: 3 :: Nil) == Some(42 :: 42 :: 42 :: Nil)

/////////////////////////////////////////////////////////////////////////////
// traverseX                                                               //
/////////////////////////////////////////////////////////////////////////////
@test
def traverseX01(): Bool = Option.traverseX(x -> Some(x + 1), Nil) == Some()

@test
def traverseX02(): Bool = Option.traverseX(x -> Some(x + 1), 1 :: Nil) == Some()

@test
def traverseX03(): Bool = Option.traverseX(x -> Some(x + 1), 1 :: 2 :: Nil) == Some()

@test
def traverseX04(): Bool = Option.traverseX(x -> Some(x + 1), 1 :: 2 :: 3 :: Nil) == Some()

@test
def traverseX05(): Bool = Option.traverseX(x -> if (x == 1) None else Some(x), 1 :: 2 :: 3 :: Nil) == None

@test
def traverseX06(): Bool = Option.traverseX(x -> if (x == 3) None else Some(x), 1 :: 2 :: 3 :: Nil) == None

@test
def traverseX07(): Bool = Option.traverseX(x -> if (x == 2) None else Some(x), 1 :: 2 :: 3 :: Nil) == None

@test
def traverseX08(): Bool = Option.traverseX(_ -> Some(42), 1 :: 2 :: 3 :: Nil) == Some()

/////////////////////////////////////////////////////////////////////////////
// foldLeftM                                                               //
/////////////////////////////////////////////////////////////////////////////
@test
def foldLeftM01(): Bool = Option.foldLeftM((ac, i) -> if (i < 0) None else Some(Int32.max(i, ac)), 0, Nil) == Some(0)

@test
def foldLeftM02(): Bool = Option.foldLeftM((ac, i) -> if (i < 0) None else Some(Int32.max(i, ac)), 0, 1 :: Nil) == Some(1)

@test
def foldLeftM03(): Bool = Option.foldLeftM((ac, i) -> if (i < 0) None else Some(Int32.max(i, ac)), 0, 1 :: 2 :: Nil) == Some(2)

@test
def foldLeftM04(): Bool = Option.foldLeftM((ac, i) -> if (i < 0) None else Some(Int32.max(i, ac)), 0, 1 :: 2 :: 3 :: Nil) == Some(3)

@test
def foldLeftM05(): Bool = Option.foldLeftM((ac, i) -> if (i < 0) None else Some(Int32.max(i, ac)), 0, -1 :: 2 :: 3 :: Nil) == None

@test
def foldLeftM06(): Bool = Option.foldLeftM((ac, i) -> if (i < 0) None else Some(Int32.max(i, ac)), 0, 1 :: -2 :: 3 :: Nil) == None

@test
def foldLeftM07(): Bool = Option.foldLeftM((ac, i) -> if (i < 0) None else Some(Int32.max(i, ac)), 0, 1 :: 2 :: -3 :: Nil) == None

/////////////////////////////////////////////////////////////////////////////
// foldRightM                                                              //
/////////////////////////////////////////////////////////////////////////////
@test
def foldRightM01(): Bool = Option.foldRightM((i, ac) -> if (i < 0) None else Some(Int32.max(i, ac)), 0, Nil) == Some(0)

@test
def foldRightM02(): Bool = Option.foldRightM((i, ac) -> if (i < 0) None else Some(Int32.max(i, ac)), 0, 1 :: Nil) == Some(1)

@test
def foldRightM03(): Bool = Option.foldRightM((i, ac) -> if (i < 0) None else Some(Int32.max(i, ac)), 0, 1 :: 2 :: Nil) == Some(2)

@test
def foldRightM04(): Bool = Option.foldRightM((i, ac) -> if (i < 0) None else Some(Int32.max(i, ac)), 0, 1 :: 2 :: 3 :: Nil) == Some(3)

@test
def foldRightM05(): Bool = Option.foldRightM((i, ac) -> if (i < 0) None else Some(Int32.max(i, ac)), 0, -1 :: 2 :: 3 :: Nil) == None

@test
def foldRightM06(): Bool = Option.foldRightM((i, ac) -> if (i < 0) None else Some(Int32.max(i, ac)), 0, 1 :: -2 :: 3 :: Nil) == None

@test
def foldRightM07(): Bool = Option.foldRightM((i, ac) -> if (i < 0) None else Some(Int32.max(i, ac)), 0, 1 :: 2 :: -3 :: Nil) == None

/////////////////////////////////////////////////////////////////////////////
// zip                                                                     //
/////////////////////////////////////////////////////////////////////////////
@test
def zip01(): Bool = Option.zip(None: Option[Unit], None: Option[Unit]) == None

@test
def zip02(): Bool = Option.zip(Some(1), None: Option[Unit]) == None

@test
def zip03(): Bool = Option.zip(None: Option[Unit], Some(true)) == None

@test
def zip04(): Bool = Option.zip(Some(1), Some(true)) == Some((1, true))

/////////////////////////////////////////////////////////////////////////////
// unzip                                                                   //
/////////////////////////////////////////////////////////////////////////////
@test
def unzip01(): Bool = Option.unzip(None: Option[(Unit, Unit)]) == (None, None)

@test
def unzip02(): Bool = Option.unzip(Some((1, true))) == (Some(1), Some(true))

/////////////////////////////////////////////////////////////////////////////
// forEach                                                                 //
/////////////////////////////////////////////////////////////////////////////

@test
def forEach01(): Bool \ IO =
    let r = ref 21;
    Option.forEach(x -> r := x, None);
    21 == deref r

@test
def forEach02(): Bool \ IO =
    let r = ref 21;
    Option.forEach(x -> r := x, Some(42));
    42 == deref r

/////////////////////////////////////////////////////////////////////////////
// point                                                                   //
/////////////////////////////////////////////////////////////////////////////

@test
def point01(): Bool = Option.point(5) == Some(5)

/////////////////////////////////////////////////////////////////////////////
// ap                                                                      //
/////////////////////////////////////////////////////////////////////////////

@test
def ap01(): Bool = Option.ap(Some(x -> x + 1), Some(123)) == Some(124)

@test
def ap02(): Bool = Option.ap(Some(x -> x + 1), None: Option[Int32]) == None

@test
def ap03(): Bool = Option.ap(None, Some(123)): Option[Int32] == None

@test
def ap04(): Bool = Option.ap(None, None): Option[Unit] == None

/////////////////////////////////////////////////////////////////////////////
// flatMap                                                                 //
/////////////////////////////////////////////////////////////////////////////

@test
def flatMap01(): Bool = Option.flatMap(x -> Some(x + 1), Some(123)) == Some(124)

@test
def flatMap02(): Bool = Option.flatMap(x -> Some(x + 1), None: Option[Int32]) == None

@test
def flatMap03(): Bool = Option.flatMap(_x -> None, Some(123)): Option[Int32] == None

@test
def flatMap04(): Bool = Option.flatMap(_x -> None, None): Option[Unit] == None

@test
def flatMap05(): Bool = Option.flatMap(i -> if (i == 1) None else Some(2*i), None) == None

@test
def flatMap06(): Bool = Option.flatMap(i -> if (i == 1) None else Some(2*i), Some(1)) == None

@test
def flatMap07(): Bool = Option.flatMap(i -> if (i == 1) None else Some(2*i), Some(2)) == Some(4)

/////////////////////////////////////////////////////////////////////////////
// lift2                                                                   //
/////////////////////////////////////////////////////////////////////////////

@test
def lift201(): Bool = Option.lift2((x1,x2) -> x1 + x2, Some(123), Some(1)) == Some(124)

@test
def lift202(): Bool = Option.lift2((x1,x2) -> x1 + x2, Some(123), None) == None

@test
def lift203(): Bool = Option.lift2((x1,x2) -> x1 + x2, None, Some(1)) == None

@test
def lift204(): Bool = Option.lift2((x1,x2) -> x1 + x2, None, None: Option[Int32]) == None

/////////////////////////////////////////////////////////////////////////////
// lift3                                                                   //
/////////////////////////////////////////////////////////////////////////////

@test
def lift301(): Bool = Option.lift3((x1,x2,x3) -> x1 + x2 + x3, Some(123), Some(1), Some(2)) == Some(126)

@test
def lift302(): Bool = Option.lift3((x1,x2,x3) -> x1 + x2 + x3, Some(123), Some(1), None) == None

@test
def lift303(): Bool = Option.lift3((x1,x2,x3) -> x1 + x2 + x3, Some(123), None, Some(2)) == None

@test
def lift304(): Bool = Option.lift3((x1,x2,x3) -> x1 + x2 + x3, None, Some(1), Some(2)) == None

@test
def lift305(): Bool = Option.lift3((x1,x2,x3) -> x1 + x2 + x3, Some(123), None, None) == None

@test
def lift306(): Bool = Option.lift3((x1,x2,x3) -> x1 + x2 + x3, None, Some(1), None) == None

@test
def lift307(): Bool = Option.lift3((x1,x2,x3) -> x1 + x2 + x3, None, None, None: Option[Int32]) == None

/////////////////////////////////////////////////////////////////////////////
// lift4                                                                   //
/////////////////////////////////////////////////////////////////////////////

@test
def lift401(): Bool = Option.lift4((x1,x2,x3,x4) -> x1 + x2 + x3 + x4, Some(123), Some(1), Some(2), Some(3)) == Some(129)

@test
def lift402(): Bool = Option.lift4((x1,x2,x3,x4) -> x1 + x2 + x3 + x4, Some(123), Some(1), Some(2), None) == None

@test
def lift403(): Bool = Option.lift4((x1,x2,x3,x4) -> x1 + x2 + x3 + x4, Some(123), Some(1), None, Some(3)) == None

@test
def lift404(): Bool = Option.lift4((x1,x2,x3,x4) -> x1 + x2 + x3 + x4, Some(123), None, Some(2), Some(3)) == None

@test
def lift405(): Bool = Option.lift4((x1,x2,x3,x4) -> x1 + x2 + x3 + x4, None, Some(1), Some(2), Some(3)) == None

@test
def lift406(): Bool = Option.lift4((x1,x2,x3,x4) -> x1 + x2 + x3 + x4, None, None, None, None: Option[Int32]) == None

/////////////////////////////////////////////////////////////////////////////
// lift5                                                                   //
/////////////////////////////////////////////////////////////////////////////

@test
def lift501(): Bool = Option.lift5((x1,x2,x3,x4,x5) -> x1 + x2 + x3 + x4 + x5, Some(123), Some(1), Some(2), Some(3), Some(4)) == Some(133)

@test
def lift502(): Bool = Option.lift5((x1,x2,x3,x4,x5) -> x1 + x2 + x3 + x4 + x5, Some(123), Some(1), Some(2), Some(3), None) == None

@test
def lift503(): Bool = Option.lift5((x1,x2,x3,x4,x5) -> x1 + x2 + x3 + x4 + x5, Some(123), Some(1), Some(2), None, Some(4)) == None

@test
def lift504(): Bool = Option.lift5((x1,x2,x3,x4,x5) -> x1 + x2 + x3 + x4 + x5, Some(123), Some(1), None, Some(3), Some(4)) == None

@test
def lift505(): Bool = Option.lift5((x1,x2,x3,x4,x5) -> x1 + x2 + x3 + x4 + x5, Some(123), None, Some(2), Some(3), Some(4)) == None

@test
def lift506(): Bool = Option.lift5((x1,x2,x3,x4,x5) -> x1 + x2 + x3 + x4 + x5, None, Some(1), Some(2), Some(3), Some(4)) == None

@test
def lift507(): Bool = Option.lift5((x1,x2,x3,x4,x5) -> x1 + x2 + x3 + x4 + x5, None, None, None, None, None: Option[Int32]) == None

/////////////////////////////////////////////////////////////////////////////
// lift6                                                                   //
/////////////////////////////////////////////////////////////////////////////

@test
def lift601(): Bool = Option.lift6((x1,x2,x3,x4,x5,x6) -> x1 + x2 + x3 + x4 + x5 + x6, Some(123), Some(1), Some(2), Some(3), Some(4), Some(5)) == Some(138)

@test
def lift602(): Bool = Option.lift6((x1,x2,x3,x4,x5,x6) -> x1 + x2 + x3 + x4 + x5 + x6, Some(123), Some(1), Some(2), Some(3), Some(4), None) == None

@test
def lift603(): Bool = Option.lift6((x1,x2,x3,x4,x5,x6) -> x1 + x2 + x3 + x4 + x5 + x6, Some(123), Some(1), Some(2), Some(3), None, Some(5)) == None

@test
def lift604(): Bool = Option.lift6((x1,x2,x3,x4,x5,x6) -> x1 + x2 + x3 + x4 + x5 + x6, Some(123), Some(1), Some(2), None, Some(4), Some(5)) == None

@test
def lift605(): Bool = Option.lift6((x1,x2,x3,x4,x5,x6) -> x1 + x2 + x3 + x4 + x5 + x6, Some(123), Some(1), None, Some(3), Some(4), Some(5)) == None

@test
def lift606(): Bool = Option.lift6((x1,x2,x3,x4,x5,x6) -> x1 + x2 + x3 + x4 + x5 + x6, Some(123), None, Some(2), Some(3), Some(4), Some(5)) == None

@test
def lift607(): Bool = Option.lift6((x1,x2,x3,x4,x5,x6) -> x1 + x2 + x3 + x4 + x5 + x6, None, Some(1), Some(2), Some(3), Some(4), Some(5)) == None

@test
def lift608(): Bool = Option.lift6((x1,x2,x3,x4,x5,x6) -> x1 + x2 + x3 + x4 + x5 + x6, None, None, None, None, None, None: Option[Int32]) == None

/////////////////////////////////////////////////////////////////////////////
// lift7                                                                   //
/////////////////////////////////////////////////////////////////////////////

@test
def lift701(): Bool = Option.lift7((x1,x2,x3,x4,x5,x6,x7) -> x1 + x2 + x3 + x4 + x5 + x6 + x7, Some(123), Some(1), Some(2), Some(3), Some(4), Some(5), Some(6)) == Some(144)

@test
def lift702(): Bool = Option.lift7((x1,x2,x3,x4,x5,x6,x7) -> x1 + x2 + x3 + x4 + x5 + x6 + x7, Some(123), Some(1), Some(2), Some(3), Some(4), Some(5), None) == None

@test
def lift703(): Bool = Option.lift7((x1,x2,x3,x4,x5,x6,x7) -> x1 + x2 + x3 + x4 + x5 + x6 + x7, Some(123), Some(1), Some(2), Some(3), Some(4), None, Some(6)) == None

@test
def lift704(): Bool = Option.lift7((x1,x2,x3,x4,x5,x6,x7) -> x1 + x2 + x3 + x4 + x5 + x6 + x7, Some(123), Some(1), Some(2), Some(3), None, Some(5), Some(6)) == None

@test
def lift705(): Bool = Option.lift7((x1,x2,x3,x4,x5,x6,x7) -> x1 + x2 + x3 + x4 + x5 + x6 + x7, Some(123), Some(1), Some(2), None, Some(4), Some(5), Some(6)) == None

@test
def lift706(): Bool = Option.lift7((x1,x2,x3,x4,x5,x6,x7) -> x1 + x2 + x3 + x4 + x5 + x6 + x7, Some(123), Some(1), None, Some(3), Some(4), Some(5), Some(6)) == None

@test
def lift707(): Bool = Option.lift7((x1,x2,x3,x4,x5,x6,x7) -> x1 + x2 + x3 + x4 + x5 + x6 + x7, Some(123), None, Some(2), Some(3), Some(4), Some(5), Some(6)) == None

@test
def lift708(): Bool = Option.lift7((x1,x2,x3,x4,x5,x6,x7) -> x1 + x2 + x3 + x4 + x5 + x6 + x7, None, Some(1), Some(2), Some(3), Some(4), Some(5), Some(6)) == None

@test
def lift709(): Bool = Option.lift7((x1,x2,x3,x4,x5,x6,x7) -> x1 + x2 + x3 + x4 + x5 + x6 + x7, None, None, None, None, None, None, None: Option[Int32]) == None

/////////////////////////////////////////////////////////////////////////////
// lift8                                                                   //
/////////////////////////////////////////////////////////////////////////////

@test
def lift801(): Bool = Option.lift8((x1,x2,x3,x4,x5,x6,x7,x8) -> x1 + x2 + x3 + x4 + x5 + x6 + x7 + x8, Some(123), Some(1), Some(2), Some(3), Some(4), Some(5), Some(6), Some(7)) == Some(151)

@test
def lift802(): Bool = Option.lift8((x1,x2,x3,x4,x5,x6,x7,x8) -> x1 + x2 + x3 + x4 + x5 + x6 + x7 + x8, Some(123), Some(1), Some(2), Some(3), Some(4), Some(5), Some(6), None) == None

@test
def lift803(): Bool = Option.lift8((x1,x2,x3,x4,x5,x6,x7,x8) -> x1 + x2 + x3 + x4 + x5 + x6 + x7 + x8, Some(123), Some(1), Some(2), Some(3), Some(4), Some(5), None, Some(7)) == None

@test
def lift804(): Bool = Option.lift8((x1,x2,x3,x4,x5,x6,x7,x8) -> x1 + x2 + x3 + x4 + x5 + x6 + x7 + x8, Some(123), Some(1), Some(2), Some(3), Some(4), None, Some(6), Some(7)) == None

@test
def lift805(): Bool = Option.lift8((x1,x2,x3,x4,x5,x6,x7,x8) -> x1 + x2 + x3 + x4 + x5 + x6 + x7 + x8, Some(123), Some(1), Some(2), Some(3), None, Some(5), Some(6), Some(7)) == None

@test
def lift806(): Bool = Option.lift8((x1,x2,x3,x4,x5,x6,x7,x8) -> x1 + x2 + x3 + x4 + x5 + x6 + x7 + x8, Some(123), Some(1), Some(2), None, Some(4), Some(5), Some(6), Some(7)) == None

@test
def lift807(): Bool = Option.lift8((x1,x2,x3,x4,x5,x6,x7,x8) -> x1 + x2 + x3 + x4 + x5 + x6 + x7 + x8, Some(123), Some(1), None, Some(3), Some(4), Some(5), Some(6), Some(7)) == None

@test
def lift808(): Bool = Option.lift8((x1,x2,x3,x4,x5,x6,x7,x8) -> x1 + x2 + x3 + x4 + x5 + x6 + x7 + x8, Some(123), None, Some(2), Some(3), Some(4), Some(5), Some(6), Some(7)) == None

@test
def lift809(): Bool = Option.lift8((x1,x2,x3,x4,x5,x6,x7,x8) -> x1 + x2 + x3 + x4 + x5 + x6 + x7 + x8, None, Some(1), Some(2), Some(3), Some(4), Some(5), Some(6), Some(7)) == None

@test
def lift810(): Bool = Option.lift8((x1,x2,x3,x4,x5,x6,x7,x8) -> x1 + x2 + x3 + x4 + x5 + x6 + x7 + x8, None, None, None, None, None, None, None, None: Option[Int32]) == None

/////////////////////////////////////////////////////////////////////////////
// lift9                                                                   //
/////////////////////////////////////////////////////////////////////////////

@test
def lift901(): Bool = Option.lift9((x1,x2,x3,x4,x5,x6,x7,x8,x9) -> x1 + x2 + x3 + x4 + x5 + x6 + x7 + x8 + x9, Some(123), Some(1), Some(2), Some(3), Some(4), Some(5), Some(6), Some(7), Some(8)) == Some(159)

@test
def lift902(): Bool = Option.lift9((x1,x2,x3,x4,x5,x6,x7,x8,x9) -> x1 + x2 + x3 + x4 + x5 + x6 + x7 + x8 + x9, Some(123), Some(1), Some(2), Some(3), Some(4), Some(5), Some(6), Some(7), None) == None

@test
def lift903(): Bool = Option.lift9((x1,x2,x3,x4,x5,x6,x7,x8,x9) -> x1 + x2 + x3 + x4 + x5 + x6 + x7 + x8 + x9, Some(123), Some(1), Some(2), Some(3), Some(4), Some(5), Some(6), None, Some(8)) == None

@test
def lift904(): Bool = Option.lift9((x1,x2,x3,x4,x5,x6,x7,x8,x9) -> x1 + x2 + x3 + x4 + x5 + x6 + x7 + x8 + x9, Some(123), Some(1), Some(2), Some(3), Some(4), Some(5), None, Some(7), Some(8)) == None

@test
def lift905(): Bool = Option.lift9((x1,x2,x3,x4,x5,x6,x7,x8,x9) -> x1 + x2 + x3 + x4 + x5 + x6 + x7 + x8 + x9, Some(123), Some(1), Some(2), Some(3), Some(4), None, Some(6), Some(7), Some(8)) == None

@test
def lift906(): Bool = Option.lift9((x1,x2,x3,x4,x5,x6,x7,x8,x9) -> x1 + x2 + x3 + x4 + x5 + x6 + x7 + x8 + x9, Some(123), Some(1), Some(2), Some(3), None, Some(5), Some(6), Some(7), Some(8)) == None

@test
def lift907(): Bool = Option.lift9((x1,x2,x3,x4,x5,x6,x7,x8,x9) -> x1 + x2 + x3 + x4 + x5 + x6 + x7 + x8 + x9, Some(123), Some(1), Some(2), None, Some(4), Some(5), Some(6), Some(7), Some(8)) == None

@test
def lift908(): Bool = Option.lift9((x1,x2,x3,x4,x5,x6,x7,x8,x9) -> x1 + x2 + x3 + x4 + x5 + x6 + x7 + x8 + x9, Some(123), Some(1), None, Some(3), Some(4), Some(5), Some(6), Some(7), Some(8)) == None

@test
def lift909(): Bool = Option.lift9((x1,x2,x3,x4,x5,x6,x7,x8,x9) -> x1 + x2 + x3 + x4 + x5 + x6 + x7 + x8 + x9, Some(123), None, Some(2), Some(3), Some(4), Some(5), Some(6), Some(7), Some(8)) == None

@test
def lift910(): Bool = Option.lift9((x1,x2,x3,x4,x5,x6,x7,x8,x9) -> x1 + x2 + x3 + x4 + x5 + x6 + x7 + x8 + x9, None, Some(1), Some(2), Some(3), Some(4), Some(5), Some(6), Some(7), Some(8)) == None

@test
def lift911(): Bool = Option.lift9((x1,x2,x3,x4,x5,x6,x7,x8,x9) -> x1 + x2 + x3 + x4 + x5 + x6 + x7 + x8 + x9, None, None, None, None, None, None, None, None, None: Option[Int32]) == None

/////////////////////////////////////////////////////////////////////////////
// lift10                                                                   //
/////////////////////////////////////////////////////////////////////////////

@test
def lift1001(): Bool = Option.lift10((x1,x2,x3,x4,x5,x6,x7,x8,x9,x10) -> x1 + x2 + x3 + x4 + x5 + x6 + x7 + x8 + x9 + x10, Some(123), Some(1), Some(2), Some(3), Some(4), Some(5), Some(6), Some(7), Some(8), Some(9)) == Some(168)

@test
def lift1002(): Bool = Option.lift10((x1,x2,x3,x4,x5,x6,x7,x8,x9,x10) -> x1 + x2 + x3 + x4 + x5 + x6 + x7 + x8 + x9 + x10, Some(123), Some(1), Some(2), Some(3), Some(4), Some(5), Some(6), Some(7), Some(8), None) == None

@test
def lift1003(): Bool = Option.lift10((x1,x2,x3,x4,x5,x6,x7,x8,x9,x10) -> x1 + x2 + x3 + x4 + x5 + x6 + x7 + x8 + x9 + x10, Some(123), Some(1), Some(2), Some(3), Some(4), Some(5), Some(6), Some(7), None, Some(9)) == None

@test
def lift1004(): Bool = Option.lift10((x1,x2,x3,x4,x5,x6,x7,x8,x9,x10) -> x1 + x2 + x3 + x4 + x5 + x6 + x7 + x8 + x9 + x10, Some(123), Some(1), Some(2), Some(3), Some(4), Some(5), Some(6), None, Some(8), Some(9)) == None

@test
def lift1005(): Bool = Option.lift10((x1,x2,x3,x4,x5,x6,x7,x8,x9,x10) -> x1 + x2 + x3 + x4 + x5 + x6 + x7 + x8 + x9 + x10, Some(123), Some(1), Some(2), Some(3), Some(4), Some(5), None, Some(7), Some(8), Some(9)) == None

@test
def lift1006(): Bool = Option.lift10((x1,x2,x3,x4,x5,x6,x7,x8,x9,x10) -> x1 + x2 + x3 + x4 + x5 + x6 + x7 + x8 + x9 + x10, Some(123), Some(1), Some(2), Some(3), Some(4), None, Some(6), Some(7), Some(8), Some(9)) == None

@test
def lift1007(): Bool = Option.lift10((x1,x2,x3,x4,x5,x6,x7,x8,x9,x10) -> x1 + x2 + x3 + x4 + x5 + x6 + x7 + x8 + x9 + x10, Some(123), Some(1), Some(2), Some(3), None, Some(5), Some(6), Some(7), Some(8), Some(9)) == None

@test
def lift1008(): Bool = Option.lift10((x1,x2,x3,x4,x5,x6,x7,x8,x9,x10) -> x1 + x2 + x3 + x4 + x5 + x6 + x7 + x8 + x9 + x10, Some(123), Some(1), Some(2), None, Some(4), Some(5), Some(6), Some(7), Some(8), Some(9)) == None

@test
def lift1009(): Bool = Option.lift10((x1,x2,x3,x4,x5,x6,x7,x8,x9,x10) -> x1 + x2 + x3 + x4 + x5 + x6 + x7 + x8 + x9 + x10, Some(123), Some(1), None, Some(3), Some(4), Some(5), Some(6), Some(7), Some(8), Some(9)) == None

@test
def lift1010(): Bool = Option.lift10((x1,x2,x3,x4,x5,x6,x7,x8,x9,x10) -> x1 + x2 + x3 + x4 + x5 + x6 + x7 + x8 + x9 + x10, Some(123), None, Some(2), Some(3), Some(4), Some(5), Some(6), Some(7), Some(8), Some(9)) == None

@test
def lift1011(): Bool = Option.lift10((x1,x2,x3,x4,x5,x6,x7,x8,x9,x10) -> x1 + x2 + x3 + x4 + x5 + x6 + x7 + x8 + x9 + x10, None, Some(1), Some(2), Some(3), Some(4), Some(5), Some(6), Some(7), Some(8), Some(9)) == None

@test
def lift1012(): Bool = Option.lift10((x1,x2,x3,x4,x5,x6,x7,x8,x9,x10) -> x1 + x2 + x3 + x4 + x5 + x6 + x7 + x8 + x9 + x10, None, None, None, None, None, None, None, None, None, None: Option[Int32]) == None

    /////////////////////////////////////////////////////////////////////////////
    // hash                                                                    //
    /////////////////////////////////////////////////////////////////////////////

    @test
    def hash01(): Bool = hash(None: Option[Unit]) == hash(None: Option[Unit])

    @test
    def hash02(): Bool = hash(Some(1)) == hash(Some(1))

    @test
    def hash03(): Bool = hash(Some((1, 1))) == hash(Some((1, 1)))

    @test
    def hash04(): Bool = hash(Some((1, 2))) != hash(Some((2, 1)))

    @test
    def hash05(): Bool = hash(None: Option[Unit]) != hash(Some((())))


    /////////////////////////////////////////////////////////////////////////////
    // iterator                                                              //
    /////////////////////////////////////////////////////////////////////////////

    @test
    def iterator01(): Bool = region r {
        None: Option[Int32] |> Option.iterator(r) |> Iterator.toList == Nil
    }

    @test
    def iterator02(): Bool = region r {
        Some(1) |> Option.iterator(r) |> Iterator.toList == 1 :: Nil
    }

    @test
    def iterator03(): Bool = region r {
        Some(1 :: 2 :: 3 :: Nil) |> Option.iterator(r) |> Iterator.toList == (1 :: 2 :: 3 :: Nil) :: Nil
    }

}
