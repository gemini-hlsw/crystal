// Copyright (c) 2016-2025 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal.react.hooks

import japgolly.scalajs.react.*
import japgolly.scalajs.react.test.ReactTestUtils
import japgolly.scalajs.react.testing_library.dom.Simulate
import japgolly.scalajs.react.vdom.html_<^.*
import munit.CatsEffectSuite
import org.scalajs.dom

// Exercises `useStateViewWithReuse` against its README description: "Similar to `useStateView` but
// returns a `Reuse[View[A]]`. The resulting `View` is reused by its value."
class UseStateViewWithReuseSpec extends CatsEffectSuite:
  import ReactTestUtils.*

  test("useStateViewWithReuse - exposes state as a reusable, modifiable View"):
    val buttonRef = Ref[dom.HTMLButtonElement]

    val comp = ScalaFnComponent[Unit]: _ =>
      for v <- useStateViewWithReuse(0)
      yield <.button(
        v.value.get.toString,
        ^.onClick --> v.value.mod(_ + 1),
        ^.onDoubleClick --> v.value.set(10)
      ).withRef(buttonRef)

    withRendered(comp()): d =>
      d.innerHTML.assert("0")
      for
        _ <- act_(Simulate.click(buttonRef.unsafeGet()))
        _  = d.innerHTML.assert("1")
        _ <- act_(Simulate.doubleClick(buttonRef.unsafeGet()))
        _  = d.innerHTML.assert("10")
      yield ()

  test("useStateViewWithReuse - the returned View is reused by value"):
    val buttonRef = Ref[dom.HTMLButtonElement]

    val comp = ScalaFnComponent[Unit]: _ =>
      for
        v          <- useStateViewWithReuse(0)
        other      <- useState(0) // an unrelated regular state, used to force re-renders
        effectRuns <- useState(0)
        // The View is reused by its value, so an effect gated on it must NOT re-run on unrelated
        // re-renders (same value), only when the value actually changes.
        _          <- useEffectWithDeps(v)(_ => effectRuns.modState(_ + 1))
      yield <.button(
        (v.value.get, other.value, effectRuns.value).toString,
        ^.onClick --> v.value.mod(_ + 1),         // value change -> effect re-runs
        ^.onDoubleClick --> other.modState(_ + 1) // unrelated re-render -> effect must NOT re-run
      ).withRef(buttonRef)

    withRendered(comp()): d =>
      d.innerHTML.assert("(0,0,1)") // effect ran once on mount
      for
        _ <- act_(Simulate.doubleClick(buttonRef.unsafeGet())) // unrelated state change
        _  = d.innerHTML.assert("(0,1,1)")                     // value unchanged -> effect did not re-run
        _ <- act_(Simulate.click(buttonRef.unsafeGet()))       // value change
        _  = d.innerHTML.assert("(1,1,2)")                     // value changed -> effect re-ran
      yield ()
