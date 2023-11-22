// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal

import cats.effect.IO
import cats.effect.testkit.TestControl
import cats.syntax.all.*
import fs2.Stream
import munit.CatsEffectSuite

import scala.concurrent.duration.*

class DeglitcherSuite extends CatsEffectSuite {

  def server[A](values: (A, FiniteDuration)*): Stream[IO, A] =
    Stream
      .emits(values)
      .evalMap { (a, t) =>
        IO.sleep(t).as(a)
      }
      .prefetchN(Int.MaxValue)

  test("emits elements immediately if unthrottled") {
    TestControl
      .executeEmbed {
        Deglitcher[IO](100.millis).flatMap { deglitcher =>
          server(() -> 1.millis, () -> 1.millis, () -> 1.millis)
            .through(deglitcher.debounce)
            .evalMap(_ => IO.realTime)
            .compile
            .toList
        }
      }
      .assertEquals(List(1.millis, 2.millis, 3.millis))
  }

  test("respects throttling") {
    TestControl
      .executeEmbed {
        Deglitcher[IO](100.millis).flatMap { deglitcher =>
          server(0 -> 0.millis, 1 -> 10.millis, 2 -> 20.millis, 3 -> 200.millis)
            .through(deglitcher.debounce)
            .evalMap(IO.realTime.tupleLeft(_))
            .compile
            .toList
            .background
            .use { result =>
              IO.sleep(1.millis) *> deglitcher.throttle *> result.flatMap(_.embedError)
            }
        }
      }
      .assertEquals(List(0 -> 0.millis, 2 -> 101.millis, 3 -> 230.millis))
  }

}
