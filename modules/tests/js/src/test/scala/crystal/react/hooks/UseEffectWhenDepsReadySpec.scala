// Copyright (c) 2016-2025 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal.react.hooks

import crystal.*
import japgolly.scalajs.react.*
import japgolly.scalajs.react.test.ReactTestUtils
import japgolly.scalajs.react.testing_library.dom.Simulate
import japgolly.scalajs.react.vdom.html_<^.*
import munit.CatsEffectSuite
import org.scalajs.dom

// Exercises `useEffectWhenDepsReady` against its README description: "It will trigger the effect only
// when the dependency transitions to a `Ready` state, whether it was from `Pending` or `Error`."
// The dependency is driven through React state and the effect records into React state, so every
// transition is observed synchronously via `act_` (no wall-clock sleeps needed).
class UseEffectWhenDepsReadySpec extends CatsEffectSuite:
  import ReactTestUtils.*

  test("useEffectWhenDepsReady - runs only on transitions into Ready"):
    val buttonRef = Ref[dom.HTMLButtonElement]

    val comp = ScalaFnComponent[Unit]: _ =>
      for
        dep <- useState(Pot.pending[Int])
        log <- useState(List.empty[Int])
        _   <- useEffectWhenDepsReady(dep.value)(v => log.modState(v :: _))
      yield <.button(
        log.value.toString,
        ^.onClick --> dep.setState(7.ready),
        ^.onDoubleClick --> dep.setState(Pot.error(RuntimeException("boom")))
      ).withRef(buttonRef)

    withRendered(comp()): d =>
      d.innerHTML.assert("List()") // not run while Pending
      for
        _ <- act_(Simulate.click(buttonRef.unsafeGet()))       // Pending -> Ready(7)
        _  = d.innerHTML.assert("List(7)")                     // ran on transition to Ready
        _ <- act_(Simulate.doubleClick(buttonRef.unsafeGet())) // Ready -> Error
        _  = d.innerHTML.assert("List(7)")                     // no run for Error
        _ <- act_(Simulate.click(buttonRef.unsafeGet()))       // Error -> Ready(7)
        _  = d.innerHTML.assert("List(7, 7)")                  // runs again on re-transition to Ready
      yield ()

  test("useEffectWhenDepsReadyOrChange - also runs when the value changes while Ready"):
    val buttonRef = Ref[dom.HTMLButtonElement]

    val comp = ScalaFnComponent[Unit]: _ =>
      for
        dep <- useState(Pot.pending[Int])
        log <- useState(List.empty[Int])
        _   <- useEffectWhenDepsReadyOrChange(dep.value)(v => log.modState(v :: _))
      yield <.button(
        log.value.toString,
        // pending -> Ready(1), then increments the Ready value on each further click
        ^.onClick --> dep.modState(_.toOption.fold(1)(_ + 1).ready)
      ).withRef(buttonRef)

    withRendered(comp()): d =>
      d.innerHTML.assert("List()") // not run while Pending
      for
        _ <- act_(Simulate.click(buttonRef.unsafeGet())) // Pending -> Ready(1)
        _  = d.innerHTML.assert("List(1)")               // ran on transition to Ready
        _ <- act_(Simulate.click(buttonRef.unsafeGet())) // Ready(1) -> Ready(2)
        _  = d.innerHTML.assert("List(2, 1)")            // also runs on value change (vs WhenDepsReady)
      yield ()
