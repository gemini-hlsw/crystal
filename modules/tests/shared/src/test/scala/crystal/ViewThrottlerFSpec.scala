// Copyright (c) 2016-2025 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal

import cats.arrow.FunctionK
import cats.effect.IO
import cats.effect.testkit.TestControl
import fs2.Stream
import munit.CatsEffectSuite

import scala.concurrent.duration.*

class ViewThrottlerFSpec extends munit.CatsEffectSuite:
  def server[A](values: (A => A, FiniteDuration)*): Stream[IO, A => A] =
    Stream
      .emits(values)
      .evalMap: (mod, t) =>
        IO.sleep(t).as(mod)
      .prefetchN(Int.MaxValue)

  def plus(a: Int): Int => Int = _ + a

  def buildViews(
    timeout: FiniteDuration
  ): IO[(ViewF[IO, Int], ViewF[IO, Int], IO[List[(Int, FiniteDuration)]])] =
    for
      view      <- AccumulatingViewF.of[IO, Int](0)
      throttler <- ViewThrottlerF[IO, IO, Int](timeout, FunctionK.id[IO], identity)
    yield
      val throttlingView = throttler.throttle(view)
      (throttlingView.throttlerView, throttlingView.throttledView, view.accumulated.map(_.toList))

  test("server modifies immediately if unthrottled"):
    TestControl
      .executeEmbed:
        for
          (_, throttledView, getAccum) <- buildViews(10.millis)
          _                            <-
            server(plus(1) -> 1.millis, plus(1) -> 1.millis, plus(1) -> 1.millis)
              .evalMap(throttledView.mod)
              .compile
              .drain
          accum                        <- getAccum
        yield accum.toList
      .assertEquals(List(0 -> 0.millis, 1 -> 1.millis, 2 -> 2.millis, 3 -> 3.millis))

  test("respects throttling") {
    TestControl
      .executeEmbed:
        for
          (throttlerView, throttledView, getAccum) <- buildViews(100.millis)
          _                                        <-
            server(plus(1) -> 10.millis, plus(1) -> 20.millis, plus(1) -> 200.millis)
              .evalMap(throttledView.mod)
              .compile
              .drain
              .background
              .use: result =>
                IO.sleep(1.millis) >> throttlerView.mod(plus(1)) >> result.flatMap(_.embedError)
          accum                                    <- getAccum
        yield accum.toList
      .assertEquals(List(0 -> 0.millis, 1 -> 1.millis, 3 -> 101.millis, 4 -> 230.millis))
  }

  test("respects multiple throttles") {
    TestControl
      .executeEmbed:
        for
          (throttlerView, throttledView, getAccum) <- buildViews(100.millis)
          _                                        <-
            server(plus(1) -> 20.millis)
              .evalMap(throttledView.mod)
              .compile
              .drain
              .background
              .use: result =>
                IO.sleep(10.millis) >> throttlerView.mod(plus(1)) >>
                  IO.sleep(40.millis) >> throttlerView.mod(plus(1)) >>
                  result.flatMap(_.embedError)
          _                                        <- IO.sleep(101.millis) // Wait for background threads
          accum                                    <- getAccum
        yield accum.toList
      .assertEquals(List(0 -> 0.millis, 1 -> 10.millis, 2 -> 50.millis, 3 -> 150.millis))
  }
