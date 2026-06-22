// Copyright (c) 2016-2025 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal.react.hooks

import cats.effect.Deferred
import cats.effect.IO
import japgolly.scalajs.react.*
import japgolly.scalajs.react.test.ReactTestUtils
import japgolly.scalajs.react.testing_library.dom.Simulate
import japgolly.scalajs.react.vdom.html_<^.*
import munit.CatsEffectSuite
import org.scalajs.dom

import scala.concurrent.duration.*

// Exercises `useThrottlingStateView` against its README description: it provides a `throttlerView`
// (for the UI) and a `throttledView` (for server updates) over the same value; modifying the
// `throttlerView` pauses the `throttledView`, which accumulates updates and applies them on timeout.
// The `ViewThrottler` is allocated asynchronously on mount, so we wait for the `Pot` to become
// `Ready` with a latch (via `useEffectWhenDepsReady`) rather than a sleep.
class UseThrottlingStateViewSpec extends CatsEffectSuite:
  import ReactTestUtils.*

  test("useThrottlingStateView - throttlerView modifications update the value immediately"):
    val buttonRef = Ref[dom.HTMLButtonElement]
    val ready     = Deferred.unsafe[IO, Unit]

    val comp = ScalaFnComponent[Unit]: _ =>
      for
        tv <- useThrottlingStateView((0, 100.millis))
        _  <- useEffectWhenDepsReady(tv)(_ => ready.complete(()).void)
      yield <.button(
        tv.toOption.map(_.throttledView.get).toString,
        ^.onClick --> tv.toOption.map(_.throttlerView.mod(_ + 1)).getOrElse(Callback.empty)
      ).withRef(buttonRef)

    withRendered(comp()): d =>
      for
        _ <- act(ready.get)                              // wait until the ViewThrottler has been allocated (Pot Ready)
        _  = d.innerHTML.assert("Some(0)")
        _ <- act_(Simulate.click(buttonRef.unsafeGet())) // UI-side modification
        _  = d.innerHTML.assert("Some(1)")
      yield ()

  test("useThrottlingStateView - throttledView updates are paused after a throttlerView change"):
    val buttonRef = Ref[dom.HTMLButtonElement]
    val ready     = Deferred.unsafe[IO, Unit]

    val comp = ScalaFnComponent[Unit]: _ =>
      for
        tv     <- useThrottlingStateView((0, 100.millis))
        server <- useState(0) // bumping this simulates a server-side update via the throttledView
        _      <- useEffectWhenDepsReady(tv)(_ => ready.complete(()).void)
        _      <- useEffectWithDeps(server.value): n =>
                    if n == 0 then IO.unit
                    else tv.toOption.map(_.throttledView.set(99)).getOrElse(IO.unit)
      yield <.button(
        tv.toOption.map(_.throttledView.get).toString,
        ^.onClick --> tv.toOption.map(_.throttlerView.mod(_ + 1)).getOrElse(Callback.empty),
        ^.onDoubleClick --> server.modState(_ + 1)
      ).withRef(buttonRef)

    withRendered(comp()): d =>
      for
        _ <- act(ready.get)
        _  = d.innerHTML.assert("Some(0)")
        _ <- act_(Simulate.click(buttonRef.unsafeGet()))       // throttlerView change -> pause throttled
        _  = d.innerHTML.assert("Some(1)")                     // applied immediately
        _ <- act_(Simulate.doubleClick(buttonRef.unsafeGet())) // server update during the pause
        _  = d.innerHTML.assert("Some(1)")                     // delayed: not applied while paused
        // Wait well past the 100ms throttle timeout (3x margin) so the test is robust to slow/
        // contended CI scheduling; the throttled application can't be latched (the modify returns
        // before the delayed apply runs).
        _ <- act(IO.sleep(300.millis))
        _  = d.innerHTML.assert("Some(99)")                    // accumulated update now applied
      yield ()
