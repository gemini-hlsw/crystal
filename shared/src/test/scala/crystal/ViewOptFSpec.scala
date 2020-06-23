package crystal

import cats.implicits._
import munit.FunSuite
import cats.effect.IO
import cats.effect.concurrent.Ref
import monocle.macros.Lenses
import cats.kernel.Eq
import monocle.Optional
import cats.effect.concurrent.Deferred

class ViewOptFSpec extends FunSuite {

  val value    = WrapOpt(0.some)
  val optional = WrapOpt.aOpt[Int]

  test("ViewF[WrapOpt[Int]].zoom(Optional).mod") {
    (for {
      ref <- Ref[IO].of(value)
      view = ViewF(value, ref.update).zoom(optional)
      _   <- view.mod(_ + 1)
      get <- ref.get
    } yield assert(get === WrapOpt(1.some))).unsafeToFuture()
  }

  test("ViewF[WrapOpt[Int]].zoom(Optional).set") {
    (for {
      ref <- Ref[IO].of(value)
      view = ViewF(value, ref.update).zoom(optional)
      _   <- view.set(1)
      get <- ref.get
    } yield assert(get === WrapOpt(1.some))).unsafeToFuture()
  }

  test("ViewF[WrapOpt[Int]].zoom(Optional).modAndGet") {
    (for {
      ref <- Ref[IO].of(value)
      view = ViewF(value, ref.update).zoom(optional)
      get <- view.modAndGet(_ + 1)
    } yield assert(get === 1.some)).unsafeToFuture()
  }

  test("ViewF[WrapOpt[Int]].zoom(Optional).withOnMod(Option[Int] => IO[Unit]).mod") {
    (for {
      ref      <- Ref[IO].of(value)
      d        <- Deferred[IO, Option[Int]]
      view      = ViewF(value, ref.update).zoom(optional).withOnMod(a => d.complete(a.map(_ * 2)))
      _        <- view.mod(_ + 1)
      get      <- ref.get
      captured <- d.get
    } yield {
      assert(get === WrapOpt(1.some))
      assert(captured === 2.some)
    }).unsafeToFuture()
  }
}
