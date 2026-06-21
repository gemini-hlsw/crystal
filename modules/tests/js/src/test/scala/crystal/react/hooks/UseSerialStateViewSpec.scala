// Copyright (c) 2016-2025 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal.react.hooks

import japgolly.scalajs.react.*
import japgolly.scalajs.react.test.ReactTestUtils
import japgolly.scalajs.react.testing_library.dom.Simulate
import japgolly.scalajs.react.vdom.html_<^.*
import munit.CatsEffectSuite
import org.scalajs.dom

// Exercises `useSerialStateView` against its README description: "Version of `useSerialState` that
// returns a `Reuse[View[A]]`" (a reusable `View` over the serial state).
class UseSerialStateViewSpec extends CatsEffectSuite:
  import ReactTestUtils.*

  test("useSerialStateView - exposes the serial state as a modifiable View"):
    val buttonRef = Ref[dom.HTMLButtonElement]

    val comp = ScalaFnComponent[Unit]: _ =>
      for sv <- useSerialStateView(0)
      yield
        val v = sv.value
        <.button(
          v.get.toString,
          ^.onClick --> v.mod(_ + 1),
          ^.onDoubleClick --> v.set(10)
        ).withRef(buttonRef)

    withRendered(comp()): d =>
      d.innerHTML.assert("0")
      for
        _ <- act_(Simulate.click(buttonRef.unsafeGet()))
        _  = d.innerHTML.assert("1")
        _ <- act_(Simulate.doubleClick(buttonRef.unsafeGet()))
        _  = d.innerHTML.assert("10")
      yield ()

  test("useSerialStateView - the returned View is reused until its value changes"):
    val buttonRef = Ref[dom.HTMLButtonElement]

    val comp = ScalaFnComponent[Unit]: _ =>
      for
        sv         <- useSerialStateView(0)
        other      <- useState(0) // an unrelated regular state, used to force re-renders
        effectRuns <- useState(0)
        // Gate an effect on the serial-state View. Because the View is reused while its value is
        // unchanged, the effect must NOT re-run on unrelated re-renders, only when the value changes.
        _          <- useEffectWithDeps(sv)(_ => effectRuns.modState(_ + 1))
      yield <.button(
        (sv.value.get, other.value, effectRuns.value).toString,
        ^.onClick --> sv.value.mod(_ + 1),        // changes the value -> effect re-runs
        ^.onDoubleClick --> other.modState(_ + 1) // unrelated re-render -> effect must NOT re-run
      ).withRef(buttonRef)

    withRendered(comp()): d =>
      d.innerHTML.assert("(0,0,1)") // effect ran once on mount
      for
        _ <- act_(Simulate.doubleClick(buttonRef.unsafeGet())) // unrelated state change
        _  = d.innerHTML.assert("(0,1,1)")                     // View reused -> effect did not re-run
        _ <- act_(Simulate.click(buttonRef.unsafeGet()))       // serial value change
        _  = d.innerHTML.assert("(1,1,2)")                     // View changed -> effect re-ran
      yield ()
