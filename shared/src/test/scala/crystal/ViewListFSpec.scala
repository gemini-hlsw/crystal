package crystal

import cats.implicits._
import munit.FunSuite
import cats.effect.IO
import cats.effect.concurrent.Ref
import cats.effect.ContextShift
import scala.concurrent.ExecutionContext
import monocle.macros.Lenses
import cats.kernel.Eq
import monocle.Optional
import monocle.Traversal
import cats.effect.concurrent.Deferred

class ViewListFSpec extends FunSuite {

  val value     = WrapList(List(0, 1, 2))
  val traversal = WrapList.aList[Int]

  test("ViewF[WrapList[Int]].zoom(Traversal).mod") {
    (for {
      ref <- Ref[IO].of(value)
      view = ViewF(value, ref.update).zoom(traversal)
      _   <- view.mod(_ + 1)
      get <- ref.get
    } yield assert(get === WrapList(List(1, 2, 3)))).unsafeToFuture()
  }

  test("ViewF[WrapList[Int]].zoom(Traversal).set") {
    (for {
      ref <- Ref[IO].of(value)
      view = ViewF(value, ref.update).zoom(traversal)
      _   <- view.set(1)
      get <- ref.get
    } yield assert(get === WrapList(List(1, 1, 1)))).unsafeToFuture()
  }

  test("ViewF[WrapList[Int]].zoom(Traversal).modAndGet") {
    (for {
      ref <- Ref[IO].of(value)
      view = ViewF(value, ref.update).zoom(traversal)
      get <- view.modAndGet(_ + 1)
    } yield assert(get === List(1, 2, 3))).unsafeToFuture()
  }

  test("ViewF[WrapList[Int]].zoom(Traversal).withOnMod(List[Int] => IO[Unit]).mod") {
    (for {
      ref      <- Ref[IO].of(value)
      d        <- Deferred[IO, List[Int]]
      view      = ViewF(value, ref.update).zoom(traversal).withOnMod(a => d.complete(a.map(_ * 2)))
      _        <- view.mod(_ + 1)
      get      <- ref.get
      captured <- d.get
    } yield {
      assert(get === WrapList(List(1, 2, 3)))
      assert(captured === List(2, 4, 6))
    }).unsafeToFuture()
  }

  val complexValue1     = Wrap(WrapOpt(WrapList(List(0, 1, 2)).some))
  val complexTraversal1 =
    Wrap
      .a[WrapOpt[WrapList[Int]]]
      .composeOptional(WrapOpt.aOpt)
      .composeTraversal(WrapList.aList)

  test("ViewF[Wrap[WrapOpt[WrapList[Int]]]].zoom(Traversal).mod") {
    (for {
      ref <- Ref[IO].of(complexValue1)
      view = ViewF(complexValue1, ref.update).zoom(complexTraversal1)
      _   <- view.mod(_ + 1)
      get <- ref.get
    } yield assert(get === Wrap(WrapOpt(WrapList(List(1, 2, 3)).some)))).unsafeToFuture()
  }

  test("ViewF[Wrap[WrapOpt[WrapList[Int]]]].zoom(Traversal).set") {
    (for {
      ref <- Ref[IO].of(complexValue1)
      view = ViewF(complexValue1, ref.update).zoom(complexTraversal1)
      _   <- view.set(1)
      get <- ref.get
    } yield assert(get === Wrap(WrapOpt(WrapList(List(1, 1, 1)).some)))).unsafeToFuture()
  }

  test("ViewF[Wrap[WrapOpt[WrapList[Int]]]].zoom(Traversal).modAndGet") {
    (for {
      ref <- Ref[IO].of(complexValue1)
      view = ViewF(complexValue1, ref.update).zoom(complexTraversal1)
      get <- view.modAndGet(_ + 1)
    } yield assert(get === List(1, 2, 3))).unsafeToFuture()
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
      .composeTraversal(WrapList.aList)
      .composeOptional(WrapOpt.aOpt[Int])

  test("ViewF[Wrap[WrapList[WrapOpt[Int]]]].zoom(Traversal).mod") {
    (for {
      ref <- Ref[IO].of(complexValue2)
      view = ViewF(complexValue2, ref.update).zoom(complexTraversal2)
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
    )).unsafeToFuture()
  }

  test("ViewF[Wrap[WrapList[WrapOpt[Int]]]].zoom(Traversal).set") {
    (for {
      ref <- Ref[IO].of(complexValue2)
      view = ViewF(complexValue2, ref.update).zoom(complexTraversal2)
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
    )).unsafeToFuture()
  }

  test("ViewF[Wrap[WrapList[WrapOpt[Int]]]].zoom(Traversal).modAndGet") {
    (for {
      ref <- Ref[IO].of(complexValue2)
      view = ViewF(complexValue2, ref.update).zoom(complexTraversal2)
      get <- view.modAndGet(_ + 1)
    } yield assert(get === List(1, 2, 3))).unsafeToFuture()
  }
}
