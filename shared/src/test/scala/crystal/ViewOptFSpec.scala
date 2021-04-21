package crystal

import cats.syntax.all._
import cats.effect.IO
import monocle.std.option.some
import munit.FunSuite
import cats.effect.{ Deferred, Ref }

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

  val valueOpt = Wrap(0).some

  test("ViewF[Option[Wrap[Int]]].zoom(some).as(Wrap.iso).mod") {
    (for {
      ref <- Ref[IO].of(valueOpt)
      view = ViewF(valueOpt, ref.update).zoom(some[Wrap[Int]]).as(Wrap.iso)
      _   <- view.mod(_ + 1)
      get <- ref.get
    } yield assert(get === Wrap(1).some)).unsafeToFuture()
  }

  test("ViewF[Option[Wrap[Int]]].zoom(some).as(Wrap.iso).set") {
    (for {
      ref <- Ref[IO].of(valueOpt)
      view = ViewF(valueOpt, ref.update).zoom(some[Wrap[Int]]).as(Wrap.iso)
      _   <- view.set(1)
      get <- ref.get
    } yield assert(get === Wrap(1).some)).unsafeToFuture()
  }

  test("ViewF[Option[Wrap[Int]]].zoom(some).as(Wrap.iso).modAndGet") {
    (for {
      ref <- Ref[IO].of(valueOpt)
      view = ViewF(valueOpt, ref.update).zoom(some[Wrap[Int]]).as(Wrap.iso)
      get <- view.modAndGet(_ + 1)
    } yield assert(get === 1.some)).unsafeToFuture()
  }

  test("ViewF[Option[Wrap[Int]]].zoom(some).asList.mod") {
    (for {
      ref <- Ref[IO].of(valueOpt)
      view = ViewF(valueOpt, ref.update).zoom(some[Wrap[Int]]).asList
      _   <- view.mod(_.map(_ + 1))
      get <- ref.get
    } yield assert(get === Wrap(1).some)).unsafeToFuture()
  }

  test("ViewF[Option[Wrap[Int]]].zoom(some).asList.set") {
    (for {
      ref <- Ref[IO].of(valueOpt)
      view = ViewF(valueOpt, ref.update).zoom(some[Wrap[Int]]).asList
      _   <- view.set(Wrap(1))
      get <- ref.get
    } yield assert(get === Wrap(1).some)).unsafeToFuture()
  }

  test("ViewF[Option[Wrap[Int]]].zoom(some).asList.modAndGet") {
    (for {
      ref <- Ref[IO].of(valueOpt)
      view = ViewF(valueOpt, ref.update).zoom(some[Wrap[Int]]).asList
      get <- view.modAndGet(_.map(_ + 1))
    } yield assert(get === List(Wrap(1)))).unsafeToFuture()
  }
}
