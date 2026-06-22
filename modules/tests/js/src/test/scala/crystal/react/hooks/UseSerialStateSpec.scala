// Copyright (c) 2016-2025 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal.react.hooks

import japgolly.scalajs.react.*
import japgolly.scalajs.react.test.ReactTestUtils
import japgolly.scalajs.react.testing_library.dom.Simulate
import japgolly.scalajs.react.vdom.html_<^.*
import munit.CatsEffectSuite
import org.scalajs.dom

// Exercises `useSerialState` against its README description: "Creates component state that is reused
// as long as it's not updated", exposing `value`, `setState` and `modState`.
class UseSerialStateSpec extends CatsEffectSuite:
  import ReactTestUtils.*

  test("useSerialState - value can be read, modified and set"):
    val buttonRef = Ref[dom.HTMLButtonElement]

    val comp = ScalaFnComponent[Unit]: _ =>
      for st <- useSerialState(0)
      yield <.button(
        st.value.value.toString,
        ^.onClick --> st.modState.value(_ + 1),
        ^.onDoubleClick --> st.setState.value(10)
      ).withRef(buttonRef)

    withRendered(comp()): d =>
      d.innerHTML.assert("0")
      for
        _ <- act_(Simulate.click(buttonRef.unsafeGet()))
        _  = d.innerHTML.assert("1")
        _ <- act_(Simulate.click(buttonRef.unsafeGet()))
        _  = d.innerHTML.assert("2")
        _ <- act_(Simulate.doubleClick(buttonRef.unsafeGet()))
        _  = d.innerHTML.assert("10")
      yield ()

  test("useSerialState - the value Reusable is reused until the state is updated"):
    val buttonRef = Ref[dom.HTMLButtonElement]

    val comp = ScalaFnComponent[Unit]: _ =>
      for
        st         <- useSerialState(0)
        other      <- useState(0) // an unrelated regular state, used to force re-renders
        effectRuns <- useState(0)
        // `st.value`'s reusability depends on an internal counter bumped on every update, so an
        // effect gated on it must NOT re-run on unrelated re-renders, only after an update.
        _          <- useEffectWithDeps(st.value)(_ => effectRuns.modState(_ + 1))
      yield <.button(
        (st.value.value, other.value, effectRuns.value).toString,
        ^.onClick --> st.modState.value(_ + 1),   // update -> effect re-runs
        ^.onDoubleClick --> other.modState(_ + 1) // unrelated re-render -> effect must NOT re-run
      ).withRef(buttonRef)

    withRendered(comp()): d =>
      d.innerHTML.assert("(0,0,1)") // effect ran once on mount
      for
        _ <- act_(Simulate.doubleClick(buttonRef.unsafeGet())) // unrelated state change
        _  = d.innerHTML.assert("(0,1,1)")                     // counter unchanged -> effect did not re-run
        _ <- act_(Simulate.click(buttonRef.unsafeGet()))       // state update
        _  = d.innerHTML.assert("(1,1,2)")                     // counter bumped -> effect re-ran
      yield ()
