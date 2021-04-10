package crystal

import cats.syntax.all._
import cats.effect.IO
import munit.FunSuite
import cats.effect.{ Deferred, Ref }

class ViewFSpec extends FunSuite {

  val value = 0

  test("ViewF[Int].mod") {
    (for {
      ref <- Ref[IO].of(value)
      view = ViewF(value, ref.update)
      _   <- view.mod(_ + 1)
      get <- ref.get
    } yield assert(get === 1)).unsafeToFuture()
  }

  test("ViewF[Int].set") {
    (for {
      ref <- Ref[IO].of(value)
      view = ViewF(value, ref.update)
      _   <- view.set(1)
      get <- ref.get
    } yield assert(get === 1)).unsafeToFuture()
  }

  test("ViewF[Int].modAndGet") {
    (for {
      ref <- Ref[IO].of(value)
      view = ViewF(value, ref.update)
      get <- view.modAndGet(_ + 1)
    } yield assert(get === 1)).unsafeToFuture()
  }

  test("ViewF[Int].withOnMod(Int => IO[Unit]).mod") {
    (for {
      ref      <- Ref[IO].of(value)
      d        <- Deferred[IO, Int]
      view      = ViewF(value, ref.update).withOnMod(a => d.complete(a * 2))
      _        <- view.mod(_ + 1)
      get      <- ref.get
      captured <- d.get
    } yield {
      assert(get === 1)
      assert(captured === 2)
    }).unsafeToFuture()
  }

  val wrappedValue = Wrap(0)
  val lens         = Wrap.a[Int]

  test("ViewF[Wrap[Int]].zoom(Lens).mod") {
    (for {
      ref <- Ref[IO].of(wrappedValue)
      view = ViewF(wrappedValue, ref.update).zoom(lens)
      _   <- view.mod(_ + 1)
      get <- ref.get
    } yield assert(get === Wrap(1))).unsafeToFuture()
  }

  test("ViewF[Wrap[Int]].zoom(Lens).set") {
    (for {
      ref <- Ref[IO].of(wrappedValue)
      view = ViewF(wrappedValue, ref.update).zoom(lens)
      _   <- view.set(1)
      get <- ref.get
    } yield assert(get === Wrap(1))).unsafeToFuture()
  }

  test("ViewF[Wrap[Int]].zoom(Lens).modAndGet") {
    (for {
      ref <- Ref[IO].of(wrappedValue)
      view = ViewF(wrappedValue, ref.update).zoom(lens)
      get <- view.modAndGet(_ + 1)
    } yield assert(get === 1)).unsafeToFuture()
  }

  test("ViewF[Wrap[Int]].as(Wrap.iso).mod") {
    (for {
      ref <- Ref[IO].of(wrappedValue)
      view = ViewF(wrappedValue, ref.update).as(Wrap.iso)
      _   <- view.mod(_ + 1)
      get <- ref.get
    } yield assert(get === Wrap(1))).unsafeToFuture()
  }

  test("ViewF[Wrap[Int]].as(Wrap.iso).set") {
    (for {
      ref <- Ref[IO].of(wrappedValue)
      view = ViewF(wrappedValue, ref.update).as(Wrap.iso)
      _   <- view.set(1)
      get <- ref.get
    } yield assert(get === Wrap(1))).unsafeToFuture()
  }

  test("ViewF[Wrap[Int]].as(Wrap.iso).modAndGet") {
    (for {
      ref <- Ref[IO].of(wrappedValue)
      view = ViewF(wrappedValue, ref.update).as(Wrap.iso)
      get <- view.modAndGet(_ + 1)
    } yield assert(get === 1)).unsafeToFuture()
  }

  test("ViewF[Wrap[Int]].asOpt.zoom(Lens).mod") {
    (for {
      ref <- Ref[IO].of(wrappedValue)
      view = ViewF(wrappedValue, ref.update).asOpt.zoom(lens)
      _   <- view.mod(_ + 1)
      get <- ref.get
    } yield assert(get === Wrap(1))).unsafeToFuture()
  }

  test("ViewF[Wrap[Int]].asOpt.zoom(Lens).set") {
    (for {
      ref <- Ref[IO].of(wrappedValue)
      view = ViewF(wrappedValue, ref.update).asOpt.zoom(lens)
      _   <- view.set(1)
      get <- ref.get
    } yield assert(get === Wrap(1))).unsafeToFuture()
  }

  test("ViewF[Wrap[Int]].asOpt.zoom(Lens).modAndGet") {
    (for {
      ref <- Ref[IO].of(wrappedValue)
      view = ViewF(wrappedValue, ref.update).asOpt.zoom(lens)
      get <- view.modAndGet(_ + 1)
    } yield assert(get === 1.some)).unsafeToFuture()
  }

  test("ViewF[Wrap[Int]].asList.zoom(Lens).mod") {
    (for {
      ref <- Ref[IO].of(wrappedValue)
      view = ViewF(wrappedValue, ref.update).asList.zoom(lens)
      _   <- view.mod(_ + 1)
      get <- ref.get
    } yield assert(get === Wrap(1))).unsafeToFuture()
  }

  test("ViewF[Wrap[Int]].asList.zoom(Lens).set") {
    (for {
      ref <- Ref[IO].of(wrappedValue)
      view = ViewF(wrappedValue, ref.update).asList.zoom(lens)
      _   <- view.set(1)
      get <- ref.get
    } yield assert(get === Wrap(1))).unsafeToFuture()
  }

  test("ViewF[Wrap[Int]].asList.zoom(Lens).modAndGet") {
    (for {
      ref <- Ref[IO].of(wrappedValue)
      view = ViewF(wrappedValue, ref.update).asList.zoom(lens)
      get <- view.modAndGet(_ + 1)
    } yield assert(get === List(1))).unsafeToFuture()
  }
}
