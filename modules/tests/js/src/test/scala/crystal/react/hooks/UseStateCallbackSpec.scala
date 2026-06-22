// Copyright (c) 2016-2025 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal.react.hooks

import japgolly.scalajs.react.*
import japgolly.scalajs.react.test.ReactTestUtils
import japgolly.scalajs.react.testing_library.dom.Simulate
import japgolly.scalajs.react.vdom.html_<^.*
import munit.CatsEffectSuite
import org.scalajs.dom

// Exercises `useStateCallback` against its README description: "register a callback that will be
// ran once, the next time the state changes. The callback will be passed the new state value."
class UseStateCallbackSpec extends CatsEffectSuite:
  import ReactTestUtils.*

  test("useStateCallback - runs the registered callback once with the new value after modState"):
    val buttonRef = Ref[dom.HTMLButtonElement]

    val comp = ScalaFnComponent[Unit]: _ =>
      for
        s   <- useState(0)
        log <- useState(List.empty[Int])
        cb  <- useStateCallback(s)
      yield <.button(
        (s.value, log.value).toString,
        // Following the README example: modState(...) >> stateCallback(...). The callback fires
        // once with the resulting value.
        ^.onClick --> (s.modState(_ + 1) >> cb.value(newValue => log.modState(newValue :: _)))
      ).withRef(buttonRef)

    withRendered(comp()): d =>
      d.innerHTML.assert("(0,List())")
      for
        _ <- act_(Simulate.click(buttonRef.unsafeGet())) // s: 0->1, callback runs with 1
        _  = d.innerHTML.assert("(1,List(1))")
        _ <- act_(Simulate.click(buttonRef.unsafeGet())) // s: 1->2, fresh callback runs with 2
        _  = d.innerHTML.assert("(2,List(2, 1))")
      yield ()

  test("useStateCallback - does not run again on later changes unless re-registered"):
    val buttonRef = Ref[dom.HTMLButtonElement]

    val comp = ScalaFnComponent[Unit]: _ =>
      for
        s   <- useState(0)
        log <- useState(List.empty[Int])
        cb  <- useStateCallback(s)
      yield <.button(
        (s.value, log.value).toString,
        ^.onClick --> (s.modState(_ + 1) >> cb.value(newValue => log.modState(newValue :: _))),
        ^.onDoubleClick --> s.modState(_ + 1) // change state WITHOUT registering a callback
      ).withRef(buttonRef)

    withRendered(comp()): d =>
      d.innerHTML.assert("(0,List())")
      for
        _ <- act_(Simulate.click(buttonRef.unsafeGet()))       // register, runs with 1
        _  = d.innerHTML.assert("(1,List(1))")
        _ <- act_(Simulate.doubleClick(buttonRef.unsafeGet())) // change with no callback registered
        _  = d.innerHTML.assert("(2,List(1))")                 // log unchanged: callback already used
      yield ()
