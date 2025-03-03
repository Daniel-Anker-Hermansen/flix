namespace Test/Exp/New {

    @test
    def testNewMutList1Region01(): MutList[Int32, Static] \ IO =
        new MutList(Static): MutList[Int32, Static]

    @test
    def testNewMutList1Region02(): MutList[Int32, Static] \ IO =
        let l = new MutList(Static);
        MutList.push!(1, l);
        l

    @test
    def testNewMutList1Region03(): MutList[String, Static] \ IO =
        region _ {
            new MutList(Static): MutList[String, Static]
        }

    @test
    def testNewMutList1Region04(): Unit =
        region r {
            discard new MutList(r): MutList[String, r];
            ()
        }

    @test
    def testNewMutList1Region05(): Int32 =
        region r {
            let l = new MutList(r);
            MutList.push!(1, l);
            match MutList.pop!(l) {
                case Some(1) => 1
                case _       => unreachable!()
            }
        }

    @test
    def testNewMutList2RegionsFlat01(): MutList[Int32, Static] \ IO =
        discard region _ {
            new MutList(Static): MutList[String, Static]
        };
        region _ {
            new MutList(Static): MutList[Int32, Static]
        }

    @test
    def testNewMutList2RegionsFlat02(): MutList[Int32, Static] \ IO =
        let _ = region r {
            discard new MutList(r): MutList[String, r];
            ()
        };
        region _ {
            new MutList(Static): MutList[Int32, Static]
        }

    @test
    def testNewMutList2RegionsFlat03(): Unit \ IO =
        discard region _ {
            new MutList(Static): MutList[String, Static]
        };
        region r {
            discard new MutList(r): MutList[Int32, r];
            ()
        }

    @test
    def testNewMutList2RegionsFlat04(): Bool =
        let res1 = region r {
            discard new MutList(r): MutList[String, r];
            ()
        };
        let res2 = region r {
            discard new MutList(r): MutList[Int32, r];
            ()
        };
        res1 == () and res1 == res2

    @test
    def testNewMutList2RegionsFlat05(): Bool =
        let res1 = region r {
            let l = new MutList(r);
            MutList.push!(1, l);
            match MutList.pop!(l) {
                case Some(1) => 1
                case _       => unreachable!()
            }
        };
        let res2 = region r {
            let l = new MutList(r);
            MutList.push!(1, l);
            match MutList.pop!(l) {
                case Some(1) => 1
                case _       => unreachable!()
            }
        };
        res1 + res2 == 2

    @test
    def testNewMutList2RegionsNested01(): Int32 \ IO =
        region _ {
            region _ {
                let l = new MutList(Static);
                MutList.push!(1, l);
                match MutList.pop!(l) {
                    case Some(1) => 1
                    case _       => unreachable!()
                }
            }
        }

    @test
    def testNewMutList2RegionsNested02(): Int32 =
        region r {
            region _ {
                let l = new MutList(r);
                MutList.push!(1, l);
                match MutList.pop!(l) {
                    case Some(1) => 1
                    case _       => unreachable!()
                }
            }
        }

    @test
    def testNewMutList2RegionsNested03(): Int32 =
        region _ {
            region r2 {
                let l = new MutList(r2);
                MutList.push!(1, l);
                match MutList.pop!(l) {
                    case Some(1) => 1
                    case _       => unreachable!()
                }
            }
        }

    @test
    def testNew2MutLists2NestedRegions01(): Int32 \ IO =
        region _ {
            region _ {
                let l1 = new MutList(Static);
                let l2 = new MutList(Static);
                MutList.push!(1, l1);
                MutList.push!(2, l2);
                match (MutList.pop!(l1), MutList.pop!(l2)) {
                    case (Some(1), Some(2)) => 1 + 2
                    case _                  => unreachable!()
                }
            }
        }

    @test
    def testNew2MutLists2NestedRegions02(): Int32 \ IO =
        region r {
            region _ {
                let l1 = new MutList(r);
                let l2 = new MutList(Static);
                MutList.push!(1, l1);
                MutList.push!(2, l2);
                match (MutList.pop!(l1), MutList.pop!(l2)) {
                    case (Some(1), Some(2)) => 1 + 2
                    case _                  => unreachable!()
                }
            }
        }

    @test
    def testNew2MutLists2NestedRegions03(): Int32 =
        region r {
            region _ {
                let l1 = new MutList(r);
                let l2 = new MutList(r);
                MutList.push!(1, l1);
                MutList.push!(2, l2);
                match (MutList.pop!(l1), MutList.pop!(l2)) {
                    case (Some(1), Some(2)) => 1 + 2
                    case _                  => unreachable!()
                }
            }
        }

    @test
    def testNew2MutLists2NestedRegions04(): Int32 =
        region r1 {
            region r2 {
                let l1 = new MutList(r1);
                let l2 = new MutList(r2);
                MutList.push!(1, l1);
                MutList.push!(2, l2);
                match (MutList.pop!(l1), MutList.pop!(l2)) {
                    case (Some(1), Some(2)) => 1 + 2
                    case _                  => unreachable!()
                }
            }
        }

    @test
    def testNew2MutLists2NestedRegions05(): Int32 =
        region r1 {
            let l1 = new MutList(r1);
            MutList.push!(1, l1);
            region r2 {
                let l2 = new MutList(r2);
                MutList.push!(2, l2);
                match (MutList.pop!(l1), MutList.pop!(l2)) {
                    case (Some(1), Some(2)) => 1 + 2
                    case _                  => unreachable!()
                }
            }
        }

    @test
    def testNew2MutLists2NestedRegions06(): Int32 =
        region r1 {
            let l1 = new MutList(r1);
            region r2 {
                MutList.push!(1, l1);
                let l2 = new MutList(r2);
                MutList.push!(2, l2);
                match (MutList.pop!(l1), MutList.pop!(l2)) {
                    case (Some(1), Some(2)) => 1 + 2
                    case _                  => unreachable!()
                }
            }
        }

    @test
    def testNewMutMap1Region01(): MutMap[Int32, Int32, Static] \ IO =
        new MutMap(Static): MutMap[Int32, Int32, Static]

    @test
    def testNewMutMap1Region02(): MutMap[Int32, Int32, Static] \ IO =
        let m = new MutMap(Static);
        MutMap.put!(1, 1, m);
        m

    @test
    def testNewMutMap1Region03(): MutMap[Int32, String, Static] \ IO =
        region _ {
            new MutMap(Static): MutMap[Int32, String, Static]
        }

    @test
    def testNewMutMap1Region04(): Unit =
        region r {
            discard new MutMap(r): MutMap[Int32, String, r];
            ()
        }

    @test
    def testNewMutMap1Region05(): Int32 =
        region r {
            let m = new MutMap(r);
            MutMap.put!(1, 1, m);
            match MutMap.get(1, m) {
                case Some(1) => 1
                case _       => unreachable!()
            }
        }

    @test
    def testNewMutMap2RegionsFlat01(): MutMap[Int32, Int32, Static] \ IO =
        discard region _ {
            new MutMap(Static): MutMap[Int32, String, Static]
        };
        region _ {
            new MutMap(Static): MutMap[Int32, Int32, Static]
        }

    @test
    def testNewMutMap2RegionsFlat02(): MutMap[Int32, Int32, Static] \ IO =
        let _ = region r {
            discard new MutMap(r): MutMap[Int32, String, r];
            ()
        };
        region _ {
            new MutMap(Static): MutMap[Int32, Int32, Static]
        }

    @test
    def testNewMutMap2RegionsFlat03(): Unit \ IO =
        discard region _ {
            new MutMap(Static): MutMap[Int32, String, Static]
        };
        region r {
            discard new MutMap(r): MutMap[Int32, Int32, r];
            ()
        }

    @test
    def testNewMutMap2RegionsFlat04(): Bool =
        let res1 = region r {
            discard new MutMap(r): MutMap[Int32, String, r];
            ()
        };
        let res2 = region r {
            discard new MutMap(r): MutMap[String, Int32, r];
            ()
        };
        res1 == () and res1 == res2

    @test
    def testNewMutMap2RegionsFlat05(): Bool =
        let res1 = region r {
            let m = new MutMap(r);
            MutMap.put!(1, 1, m);
            match MutMap.get(1, m) {
                case Some(1) => 1
                case _       => unreachable!()
            }
        };
        let res2 = region r {
            let m = new MutMap(r);
            MutMap.put!(1, 1, m);
            match MutMap.get(1, m) {
                case Some(1) => 1
                case _       => unreachable!()
            }
        };
        res1 + res2 == 2

    @test
    def testNewMutMap2RegionsNested01(): Int32 \ IO =
        region _ {
            region _ {
                let m = new MutMap(Static);
                MutMap.put!(1, 1, m);
                match MutMap.get(1, m) {
                    case Some(1) => 1
                    case _       => unreachable!()
                }
            }
        }

    @test
    def testNewMutMap2RegionsNested02(): Int32 =
        region r {
            region _ {
                let m = new MutMap(r);
                MutMap.put!(1, 1, m);
                match MutMap.get(1, m) {
                    case Some(1) => 1
                    case _       => unreachable!()
                }
            }
        }

    @test
    def testNewMutMap2RegionsNested03(): Int32 =
        region _ {
            region r2 {
                let m = new MutMap(r2);
                MutMap.put!(1, 1, m);
                match MutMap.get(1, m) {
                    case Some(1) => 1
                    case _       => unreachable!()
                }
            }
        }

    @test
    def testNew2MutMaps2NestedRegions01(): Int32 \ IO =
        region _ {
            region _ {
                let m1 = new MutMap(Static);
                let m2 = new MutMap(Static);
                MutMap.put!(1, 1, m1);
                MutMap.put!(2, 2, m2);
                match (MutMap.get(1, m1), MutMap.get(2, m2)) {
                    case (Some(1), Some(2)) => 1 + 2
                    case _                  => unreachable!()
                }
            }
        }

    @test
    def testNew2MutMaps2NestedRegions02(): Int32 \ IO =
        region r {
            region _ {
                let m1 = new MutMap(r);
                let m2 = new MutMap(Static);
                MutMap.put!(1, 1, m1);
                MutMap.put!(2, 2, m2);
                match (MutMap.get(1, m1), MutMap.get(2, m2)) {
                    case (Some(1), Some(2)) => 1 + 2
                    case _                  => unreachable!()
                }
            }
        }

    @test
    def testNew2MutMaps2NestedRegions03(): Int32 =
        region r {
            region _ {
                let m1 = new MutMap(r);
                let m2 = new MutMap(r);
                MutMap.put!(1, 1, m1);
                MutMap.put!(2, 2, m2);
                match (MutMap.get(1, m1), MutMap.get(2, m2)) {
                    case (Some(1), Some(2)) => 1 + 2
                    case _                  => unreachable!()
                }
            }
        }

    @test
    def testNew2MutMaps2NestedRegions04(): Int32 =
        region r1 {
            region r2 {
                let m1 = new MutMap(r1);
                let m2 = new MutMap(r2);
                MutMap.put!(1, 1, m1);
                MutMap.put!(2, 2, m2);
                match (MutMap.get(1, m1), MutMap.get(2, m2)) {
                    case (Some(1), Some(2)) => 1 + 2
                    case _                  => unreachable!()
                }
            }
        }

    @test
    def testNew2MutMaps2NestedRegions05(): Int32 =
        region r1 {
            let m1 = new MutMap(r1);
            MutMap.put!(1, 1, m1);
            region r2 {
                let m2 = new MutMap(r2);
                MutMap.put!(2, 2, m2);
                match (MutMap.get(1, m1), MutMap.get(2, m2)) {
                    case (Some(1), Some(2)) => 1 + 2
                    case _                  => unreachable!()
                }
            }
        }

    @test
    def testNew2MutMaps2NestedRegions06(): Int32 =
        region r1 {
            let m1 = new MutMap(r1);
            region r2 {
                MutMap.put!(1, 1, m1);
                let m2 = new MutMap(r2);
                MutMap.put!(2, 2, m2);
                match (MutMap.get(1, m1), MutMap.get(2, m2)) {
                    case (Some(1), Some(2)) => 1 + 2
                    case _                  => unreachable!()
                }
            }
        }

}
