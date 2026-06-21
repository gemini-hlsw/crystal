// Copyright (c) 2016-2025 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal.react.hooks

import cats.effect.Deferred
import cats.effect.IO
import cats.effect.Resource
import japgolly.scalajs.react.*
import japgolly.scalajs.react.test.ReactTestUtils
import japgolly.scalajs.react.testing_library.dom.Simulate
import japgolly.scalajs.react.vdom.html_<^.*
import munit.CatsEffectSuite
import org.scalajs.dom

// Exercises `useResource` against its README description: "Open a `Resource` on mount or when
// dependencies change, and close it on unmount or when dependencies change. Provided as a `Pot[A]`."
// The resource opens asynchronously, so each `Ready` transition is awaited with a latch (gated on
// `useEffectWhenDepsReady`) keyed by the resource value, rather than a sleep.
class UseResourceSpec extends CatsEffectSuite:
  import ReactTestUtils.*

  test("useResource - opens on mount and closes/reopens on dependency change"):
    val opened    = cats.effect.Ref.unsafe[IO, Int](0)
    val closed    = cats.effect.Ref.unsafe[IO, Int](0)
    val ready0    = Deferred.unsafe[IO, Unit] // signalled when the resource is Ready with value 0
    val ready1    = Deferred.unsafe[IO, Unit] // signalled when the resource is Ready with value 1
    val buttonRef = Ref[dom.HTMLButtonElement]

    // Each resource signals its own acquire latch. Doing it in the acquire (pure `IO`, before any
    // React commit) lets `act` drive it to completion, unlike a latch tied to the `Pot` becoming
    // `Ready` (which needs a `setStateAsync` commit and would deadlock `act`).
    def res(id: Int): Resource[IO, Int] =
      Resource.make(
        opened.update(_ + 1) >> (if id == 0 then ready0 else ready1).complete(()).as(id)
      )(_ => closed.update(_ + 1))

    val comp = ScalaFnComponent[Unit]: _ =>
      for
        dep <- useState(0)
        r   <- useResource(dep.value)(res)
      yield <.button(r.toOption.toString, ^.onClick --> dep.modState(_ + 1)).withRef(buttonRef)

    withRendered(comp()): d =>
      for
        _  <- act(ready0.get)                             // wait until the resource has opened with value 0
        _   = d.innerHTML.assert("Some(0)")
        o1 <- opened.get
        c1 <- closed.get
        _   = assertEquals((o1, c1), (1, 0))
        _  <- act_(Simulate.click(buttonRef.unsafeGet())) // deps change -> close old, open new
        _  <- act(ready1.get)                             // wait until the resource has reopened
        _   = d.innerHTML.assert("Some(1)")
        o2 <- opened.get
        c2 <- closed.get
        _   = assertEquals((o2, c2), (2, 1))
      yield ()
