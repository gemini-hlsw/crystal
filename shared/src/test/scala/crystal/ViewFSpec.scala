package crystal

import cats.effect.Deferred
import cats.effect.IO
import cats.effect.Ref
import cats.syntax.all._

class ViewFSpec extends munit.CatsEffectSuite {

  val value = 0

  test("ViewF[Int].mod") {
    (for {
      ref <- Ref[IO].of(value)
      view = ViewF(value, refModCB(ref))
      _   <- view.mod(_ + 1)
      get <- ref.get
    } yield assert(get === 1))
  }

  test("ViewF[Int].set") {
    (for {
      ref <- Ref[IO].of(value)
      view = ViewF(value, refModCB(ref))
      _   <- view.set(1)
      get <- ref.get
    } yield assert(get === 1))
  }

  test("ViewF[Int].modAndGet") {
    (for {
      ref <- Ref[IO].of(value)
      view = ViewF(value, refModCB(ref))
      get <- view.modAndGet(_ + 1)
    } yield assert(get === 1))
  }

  test("ViewF[Int].withOnMod(Int => IO[Unit]).mod") {
    (for {
      ref      <- Ref[IO].of(value)
      d        <- Deferred[IO, Int]
      view      = ViewF(value, refModCB(ref)).withOnMod(a => d.complete(a * 2).void)
      _        <- view.mod(_ + 1)
      get      <- ref.get
      captured <- d.get
    } yield {
      assert(get === 1)
      assert(captured === 2)
    })
  }

  val wrappedValue = Wrap(0)
  val lens         = Wrap.a[Int]

  test("ViewF[Wrap[Int]].zoom(Lens).mod") {
    (for {
      ref <- Ref[IO].of(wrappedValue)
      view = ViewF(wrappedValue, refModCB(ref)).zoom(lens)
      _   <- view.mod(_ + 1)
      get <- ref.get
    } yield assert(get === Wrap(1)))
  }

  test("ViewF[Wrap[Int]].zoom(Lens).set") {
    (for {
      ref <- Ref[IO].of(wrappedValue)
      view = ViewF(wrappedValue, refModCB(ref)).zoom(lens)
      _   <- view.set(1)
      get <- ref.get
    } yield assert(get === Wrap(1)))
  }

  test("ViewF[Wrap[Int]].zoom(Lens).modAndGet") {
    (for {
      ref <- Ref[IO].of(wrappedValue)
      view = ViewF(wrappedValue, refModCB(ref)).zoom(lens)
      get <- view.modAndGet(_ + 1)
    } yield assert(get === 1))
  }

  test("ViewF[Wrap[Int]].as(Wrap.iso).mod") {
    (for {
      ref <- Ref[IO].of(wrappedValue)
      view = ViewF(wrappedValue, refModCB(ref)).as(Wrap.iso)
      _   <- view.mod(_ + 1)
      get <- ref.get
    } yield assert(get === Wrap(1)))
  }

  test("ViewF[Wrap[Int]].as(Wrap.iso).set") {
    (for {
      ref <- Ref[IO].of(wrappedValue)
      view = ViewF(wrappedValue, refModCB(ref)).as(Wrap.iso)
      _   <- view.set(1)
      get <- ref.get
    } yield assert(get === Wrap(1)))
  }

  test("ViewF[Wrap[Int]].as(Wrap.iso).modAndGet") {
    (for {
      ref <- Ref[IO].of(wrappedValue)
      view = ViewF(wrappedValue, refModCB(ref)).as(Wrap.iso)
      get <- view.modAndGet(_ + 1)
    } yield assert(get === 1))
  }

  test("ViewF[Wrap[Int]].asOpt.zoom(Lens).mod") {
    (for {
      ref <- Ref[IO].of(wrappedValue)
      view = ViewF(wrappedValue, refModCB(ref)).asOpt.zoom(lens)
      _   <- view.mod(_ + 1)
      get <- ref.get
    } yield assert(get === Wrap(1)))
  }

  test("ViewF[Wrap[Int]].asOpt.zoom(Lens).set") {
    (for {
      ref <- Ref[IO].of(wrappedValue)
      view = ViewF(wrappedValue, refModCB(ref)).asOpt.zoom(lens)
      _   <- view.set(1)
      get <- ref.get
    } yield assert(get === Wrap(1)))
  }

  test("ViewF[Wrap[Int]].asOpt.zoom(Lens).modAndGet") {
    (for {
      ref <- Ref[IO].of(wrappedValue)
      view = ViewF(wrappedValue, refModCB(ref)).asOpt.zoom(lens)
      get <- view.modAndGet(_ + 1)
    } yield assert(get === 1.some))
  }

  test("ViewF[Wrap[Int]].asList.zoom(Lens).mod") {
    (for {
      ref <- Ref[IO].of(wrappedValue)
      view = ViewF(wrappedValue, refModCB(ref)).asList.zoom(lens)
      _   <- view.mod(_ + 1)
      get <- ref.get
    } yield assert(get === Wrap(1)))
  }

  test("ViewF[Wrap[Int]].asList.zoom(Lens).set") {
    (for {
      ref <- Ref[IO].of(wrappedValue)
      view = ViewF(wrappedValue, refModCB(ref)).asList.zoom(lens)
      _   <- view.set(1)
      get <- ref.get
    } yield assert(get === Wrap(1)))
  }

  test("ViewF[Wrap[Int]].asList.zoom(Lens).modAndGet") {
    (for {
      ref <- Ref[IO].of(wrappedValue)
      view = ViewF(wrappedValue, refModCB(ref)).asList.zoom(lens)
      get <- view.modAndGet(_ + 1)
    } yield assert(get === List(1)))
  }
}
