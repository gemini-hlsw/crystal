// Copyright (c) 2016-2025 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal.react.hooks

import cats.effect.Deferred
import cats.effect.IO
import japgolly.scalajs.react.*
import japgolly.scalajs.react.test.ReactTestUtils
import japgolly.scalajs.react.vdom.html_<^.*
import munit.CatsEffectSuite

import scala.concurrent.duration.*

// Exercises `useThrottledCallback` against its README description: "Returns a memoized version of it
// that won't be run more than once every `spacedBy` duration [...] If multiple invocations occur
// within the `spacedBy` duration, only the last one will be run."
class UseThrottledCallbackSpec extends CatsEffectSuite:
  import ReactTestUtils.*

  test("useThrottledCallback - runs the first invocation, then only the last within the window"):
    val count    = cats.effect.Ref.unsafe[IO, Int](0)
    val firstRan = Deferred.unsafe[IO, Unit] // signalled when the first invocation runs
    val lastRan  = Deferred.unsafe[IO, Unit] // signalled when the throttled last invocation runs

    val effect: IO[Unit] =
      count.updateAndGet(_ + 1).flatMap {
        case 1 => firstRan.complete(()).void
        case 2 => lastRan.complete(()).void
        case _ => IO.unit
      }

    val comp = ScalaFnComponent[Unit]: _ =>
      for
        cb <- useThrottledCallback(100.millis)(effect)
        // Invoke three times in rapid succession on mount.
        _  <- useEffectOnMount(cb.value >> cb.value >> cb.value)
      yield EmptyVdom

    withRendered(comp()): _ =>
      for
        _  <- act(firstRan.get)   // wait until the first invocation has run
        c1 <- count.get
        _   = assertEquals(c1, 1) // first invocation ran immediately
        _  <- act(lastRan.get)    // wait until the throttler runs the last queued invocation
        c2 <- count.get
        _   = assertEquals(c2, 2) // only the last queued invocation ran (the middle one was dropped)
      yield ()
