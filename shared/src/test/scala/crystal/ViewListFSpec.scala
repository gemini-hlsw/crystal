package crystal

import cats.effect.Deferred
import cats.effect.IO
import cats.effect.Ref
import cats.syntax.all._
import monocle.Traversal

class ViewListFSpec extends munit.CatsEffectSuite {

  val value     = WrapList(List(0, 1, 2))
  val traversal = WrapList.aList[Int]

  test("ViewF[WrapList[Int]].zoom(Traversal).mod") {
    for {
      ref <- Ref[IO].of(value)
      view = ViewF(value, refModCB(ref)).zoom(traversal)
      _   <- view.mod(_ + 1)
      get <- ref.get
    } yield assert(get === WrapList(List(1, 2, 3)))
  }

  test("ViewF[WrapList[Int]].zoom(Traversal).set") {
    for {
      ref <- Ref[IO].of(value)
      view = ViewF(value, refModCB(ref)).zoom(traversal)
      _   <- view.set(1)
      get <- ref.get
    } yield assert(get === WrapList(List(1, 1, 1)))
  }

  test("ViewF[WrapList[Int]].zoom(Traversal).modAndGet") {
    for {
      ref <- Ref[IO].of(value)
      view = ViewF(value, refModCB(ref)).zoom(traversal)
      get <- view.modAndGet(_ + 1)
    } yield assert(get === List(1, 2, 3))
  }

  test("ViewF[WrapList[Int]].zoom(Traversal).withOnMod(List[Int] => IO[Unit]).mod") {
    for {
      ref      <- Ref[IO].of(value)
      d        <- Deferred[IO, List[Int]]
      view      =
        ViewF(value, refModCB(ref)).zoom(traversal).withOnMod(a => d.complete(a.map(_ * 2)).void)
      _        <- view.mod(_ + 1)
      get      <- ref.get
      captured <- d.get
    } yield {
      assert(get === WrapList(List(1, 2, 3)))
      assert(captured === List(2, 4, 6))
    }
  }

  val valueList = List(Wrap(0))

  test("ViewF[List[Wrap[Int]]].zoom(Traversal).as(Wrap.iso).mod") {
    for {
      ref <- Ref[IO].of(valueList)
      view =
        ViewF(valueList, refModCB(ref)).zoom(Traversal.fromTraverse[List, Wrap[Int]]).as(Wrap.iso)
      _   <- view.mod(_ + 1)
      get <- ref.get
    } yield assert(get === List(Wrap(1)))
  }

  test("ViewF[Option[Wrap[Int]]].zoom(Traversal).as(Wrap.iso).set") {
    for {
      ref <- Ref[IO].of(valueList)
      view =
        ViewF(valueList, refModCB(ref)).zoom(Traversal.fromTraverse[List, Wrap[Int]]).as(Wrap.iso)
      _   <- view.set(1)
      get <- ref.get
    } yield assert(get === List(Wrap(1)))
  }

  test("ViewF[Option[Wrap[Int]]].zoom(Traversal).as(Wrap.iso).modAndGet") {
    for {
      ref <- Ref[IO].of(valueList)
      view =
        ViewF(valueList, refModCB(ref)).zoom(Traversal.fromTraverse[List, Wrap[Int]]).as(Wrap.iso)
      get <- view.modAndGet(_ + 1)
    } yield assert(get === List(1))
  }

  val complexValue1     = Wrap(WrapOpt(WrapList(List(0, 1, 2)).some))
  val complexTraversal1 =
    Wrap
      .a[WrapOpt[WrapList[Int]]]
      .andThen(WrapOpt.aOpt[WrapList[Int]])
      .andThen(WrapList.aList[Int])

  test("ViewF[Wrap[WrapOpt[WrapList[Int]]]].zoom(Traversal).mod") {
    for {
      ref <- Ref[IO].of(complexValue1)
      view = ViewF(complexValue1, refModCB(ref)).zoom(complexTraversal1)
      _   <- view.mod(_ + 1)
      get <- ref.get
    } yield assert(get === Wrap(WrapOpt(WrapList(List(1, 2, 3)).some)))
  }

  test("ViewF[Wrap[WrapOpt[WrapList[Int]]]].zoom(Traversal).set") {
    for {
      ref <- Ref[IO].of(complexValue1)
      view = ViewF(complexValue1, refModCB(ref)).zoom(complexTraversal1)
      _   <- view.set(1)
      get <- ref.get
    } yield assert(get === Wrap(WrapOpt(WrapList(List(1, 1, 1)).some)))
  }

  test("ViewF[Wrap[WrapOpt[WrapList[Int]]]].zoom(Traversal).modAndGet") {
    for {
      ref <- Ref[IO].of(complexValue1)
      view = ViewF(complexValue1, refModCB(ref)).zoom(complexTraversal1)
      get <- view.modAndGet(_ + 1)
    } yield assert(get === List(1, 2, 3))
  }

  val complexValue2     = Wrap(
    WrapList(
      List(WrapOpt(0.some),
           WrapOpt(none[Int]),
           WrapOpt(1.some),
           WrapOpt(none[Int]),
           WrapOpt(2.some)
      )
    )
  )
  val complexTraversal2 =
    Wrap
      .a[WrapList[WrapOpt[Int]]]
      .andThen(WrapList.aList[WrapOpt[Int]])
      .andThen(WrapOpt.aOpt[Int])

  test("ViewF[Wrap[WrapList[WrapOpt[Int]]]].zoom(Traversal).mod") {
    for {
      ref <- Ref[IO].of(complexValue2)
      view = ViewF(complexValue2, refModCB(ref)).zoom(complexTraversal2)
      _   <- view.mod(_ + 1)
      get <- ref.get
    } yield assert(
      get === Wrap(
        WrapList(
          List(WrapOpt(1.some),
               WrapOpt(none[Int]),
               WrapOpt(2.some),
               WrapOpt(none[Int]),
               WrapOpt(3.some)
          )
        )
      )
    )
  }

  test("ViewF[Wrap[WrapList[WrapOpt[Int]]]].zoom(Traversal).set") {
    for {
      ref <- Ref[IO].of(complexValue2)
      view = ViewF(complexValue2, refModCB(ref)).zoom(complexTraversal2)
      _   <- view.set(1)
      get <- ref.get
    } yield assert(
      get === Wrap(
        WrapList(
          List(WrapOpt(1.some),
               WrapOpt(none[Int]),
               WrapOpt(1.some),
               WrapOpt(none[Int]),
               WrapOpt(1.some)
          )
        )
      )
    )
  }

  test("ViewF[Wrap[WrapList[WrapOpt[Int]]]].zoom(Traversal).modAndGet") {
    for {
      ref <- Ref[IO].of(complexValue2)
      view = ViewF(complexValue2, refModCB(ref)).zoom(complexTraversal2)
      get <- view.modAndGet(_ + 1)
    } yield assert(get === List(1, 2, 3))
  }
}
