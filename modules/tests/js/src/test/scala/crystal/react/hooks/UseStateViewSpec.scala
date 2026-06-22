// Copyright (c) 2016-2025 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal.react.hooks

import japgolly.scalajs.react.*
import japgolly.scalajs.react.test.ReactTestUtils
import japgolly.scalajs.react.testing_library.dom.Simulate
import japgolly.scalajs.react.vdom.html_<^.*
import munit.CatsEffectSuite
import org.scalajs.dom

// Exercises `useStateView` against its README description: "Provides state as a `View`.
// Functionally equivalent to `useState` but the `View` is more practical to pass around to child
// components, zoom into members and define callbacks upon state change."
class UseStateViewSpec extends CatsEffectSuite:
  import ReactTestUtils.*

  case class Wrapper(count: Int, name: String)

  test("useStateView - exposes state as a View that can be modified"):
    val buttonRef = Ref[dom.HTMLButtonElement]

    val comp = ScalaFnComponent[Unit]: _ =>
      for v <- useStateView(0)
      yield <.button(v.get.toString, ^.onClick --> v.mod(_ + 1)).withRef(buttonRef)

    withRendered(comp()): d =>
      d.innerHTML.assert("0")
      for
        _ <- act_(Simulate.click(buttonRef.unsafeGet()))
        _  = d.innerHTML.assert("1")
        _ <- act_(Simulate.click(buttonRef.unsafeGet()))
        _  = d.innerHTML.assert("2")
      yield ()

  test("useStateView - set replaces the value"):
    val buttonRef = Ref[dom.HTMLButtonElement]

    val comp = ScalaFnComponent[Unit]: _ =>
      for v <- useStateView(0)
      yield <.button(v.get.toString, ^.onClick --> v.set(42)).withRef(buttonRef)

    withRendered(comp()): d =>
      d.innerHTML.assert("0")
      for
        _ <- act_(Simulate.click(buttonRef.unsafeGet()))
        _  = d.innerHTML.assert("42")
      yield ()

  test("useStateView - zoom into a member modifies the parent state"):
    val buttonRef = Ref[dom.HTMLButtonElement]

    val comp = ScalaFnComponent[Unit]: _ =>
      for v <- useStateView(Wrapper(0, "a"))
      yield
        val countView = v.zoom((_: Wrapper).count)(f => w => w.copy(count = f(w.count)))
        <.button(v.get.toString, ^.onClick --> countView.mod(_ + 1)).withRef(buttonRef)

    withRendered(comp()): d =>
      d.innerHTML.assert("Wrapper(0,a)")
      for
        _ <- act_(Simulate.click(buttonRef.unsafeGet()))
        _  = d.innerHTML.assert("Wrapper(1,a)")
      yield ()

  test("useStateView - withOnMod chains an effect when modified"):
    val buttonRef = Ref[dom.HTMLButtonElement]

    val comp = ScalaFnComponent[Unit]: _ =>
      for
        v   <- useStateView(0)
        log <- useStateView(List.empty[Int])
      yield
        val tracked = v.withOnMod(newValue => log.mod(newValue :: _))
        <.button((v.get, log.get).toString, ^.onClick --> tracked.mod(_ + 1)).withRef(buttonRef)

    withRendered(comp()): d =>
      d.innerHTML.assert("(0,List())")
      for
        _ <- act_(Simulate.click(buttonRef.unsafeGet()))
        _  = d.innerHTML.assert("(1,List(1))")
        _ <- act_(Simulate.click(buttonRef.unsafeGet()))
        _  = d.innerHTML.assert("(2,List(2, 1))")
      yield ()
