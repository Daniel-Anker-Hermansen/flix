namespace Test/Exp/Scope {

    // ensure that type variables are propagated correctly within the region
    pub def f(r: Region[r], x: Unit -> Unit \ { Write(r) }): Unit = ???
    pub def g(): Unit = region r {
        f(r, () -> unsafe_cast () as _ \ { Write(r) })
    }
}
