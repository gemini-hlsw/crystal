// Copyright (c) 2016-2025 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal

import cats.effect.IO
import cats.effect.Ref
import cats.effect.testkit.TestControl
import cats.syntax.all.*
import munit.CatsEffectSuite

import scala.concurrent.duration.*

class ThrottlerSpec extends CatsEffectSuite:

  def server(
    spacedBy: FiniteDuration,
    values:   (FiniteDuration, FiniteDuration)*
  ): IO[Ref[IO, List[(FiniteDuration, FiniteDuration)]]] =
    for
      throttler <- Throttler[IO](spacedBy)
      ref       <- Ref[IO].of(List.empty[(FiniteDuration, FiniteDuration)])
      _         <- values.toList.parTraverse: (delay, duration) =>
                     IO.sleep(delay) >>
                       throttler.submit:
                         for
                           start <- IO.realTime
                           _     <- IO.sleep(duration)
                           end   <- IO.realTime
                           _     <- ref.update(_ :+ (start, end))
                         yield ()
    yield ref

  def testSequence(
    spacedBy: FiniteDuration,
    values:   (FiniteDuration, FiniteDuration)*
  )(expected: (FiniteDuration, FiniteDuration)*): IO[Unit] =
    TestControl
      .executeEmbed(server(spacedBy, values*))
      .flatMap(_.get)
      .assertEquals(expected)

  test("behaves normally if submissions are spaced by threshold or more"):
    testSequence(
      100.millis,
      (100.millis, 10.millis),
      (200.millis, 10.millis),
      (310.millis, 10.millis)
    )(
      (100.millis, 110.millis),
      (200.millis, 210.millis),
      (310.millis, 320.millis)
    )

  test("delays single effects that are spaced by less than threshold"):
    testSequence(
      100.millis,
      (100.millis, 10.millis),
      (150.millis, 10.millis),
      (201.millis, 10.millis)
    )(
      (100.millis, 110.millis),
      (200.millis, 210.millis),
      (300.millis, 310.millis)
    )

  test("drops multiple effects that are spaced by less than threshold"):
    testSequence(
      100.millis,
      (100.millis, 10.millis),
      (150.millis, 10.millis), // dropped
      (160.millis, 20.millis), // dropped
      (170.millis, 30.millis), // executed
      (201.millis, 10.millis), // dropped
      (220.millis, 20.millis), // dropped
      (290.millis, 30.millis)  // executed
    )(
      (100.millis, 110.millis),
      (200.millis, 230.millis),
      (300.millis, 330.millis)
    )

  test("drops multiple effects that are submitted while effect is running"):
    testSequence(
      10.millis,
      (100.millis, 100.millis),
      (150.millis, 110.millis), // dropped
      (160.millis, 120.millis), // dropped
      (170.millis, 130.millis), // executed
      (201.millis, 100.millis), // dropped
      (220.millis, 120.millis), // dropped
      (290.millis, 130.millis)  // executed
    )(
      (100.millis, 200.millis),
      (200.millis, 330.millis),
      (330.millis, 460.millis)
    )

  test(
    "drops multiple effects that are submitted while effect is running or spaced less than threshold"
  ):
    testSequence(
      100.millis,
      (100.millis, 80.millis),
      (150.millis, 110.millis), // dropped
      (160.millis, 120.millis), // dropped
      (190.millis, 130.millis), // dropped
      (195.millis, 140.millis), // executed
      (201.millis, 100.millis), // dropped
      (220.millis, 120.millis), // dropped
      (290.millis, 130.millis)  // executed
    )(
      (100.millis, 180.millis),
      (200.millis, 340.millis),
      (340.millis, 470.millis)
    )
