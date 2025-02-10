// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal.react.hooks

import cats.effect.IO
import cats.syntax.all.*
import japgolly.scalajs.react.*
import japgolly.scalajs.react.test.ReactTestUtils2
import japgolly.scalajs.react.test.Simulate
import japgolly.scalajs.react.vdom.html_<^.*
import munit.CatsEffectSuite
import org.scalajs.dom
import cats.effect.std.Queue
import scala.concurrent.duration.*

class UseEffectResultSpec extends CatsEffectSuite:
  import ReactTestUtils2.*

  case class MyInt(wrapped: Int):
    def inc: MyInt = copy(wrapped + 1)
  given Reusability[MyInt] = Reusability.by(_.wrapped / 2)

  def buildQueue: IO[Queue[IO, Int]] =
    for
      q <- Queue.unbounded[IO, Int]
      _ <- q.offer(1)
      _ <- q.offer(2)
      _ <- q.offer(3)
      _ <- q.offer(4)
    yield q

  test("useEffectResultOnMount - only call once"):
    val buttonRef = Ref[dom.HTMLButtonElement]

    val comp = ScalaFnComponent[Queue[IO, Int]]: q =>
      for
        s <- useState(0)
        v <- useEffectResultOnMount(IO.sleep(50.millis) >> q.take)
      yield <.button((v.value, v.isRunning).toString, ^.onClick --> s.modState(_ + 1))
        .withRef(buttonRef)

    for
      q <- buildQueue
      _ <- withRendered(comp(q)): d =>
             d.innerHTML.assert("(Pending,true)")
             for
               _ <- act(IO.sleep(50.millis))
               _  = d.innerHTML.assert("(Ready(1),false)")
               _ <- act_(Simulate.click(buttonRef.unsafeGet())) // force rerender
               _  = d.innerHTML.assert("(Ready(1),false)")
             yield ()
    yield ()

  test("useEffectResultOnMount - call when refresh invoked"):
    val buttonRef = Ref[dom.HTMLButtonElement]

    val comp = ScalaFnComponent[Queue[IO, Int]]: q =>
      for
        s <- useState(0)
        v <- useEffectResultOnMount(IO.sleep(50.millis) >> q.take)
      yield <.button(
        (v.value, v.isRunning).toString,
        ^.onClick --> v.refresh,
        ^.onDoubleClick --> v.refreshKeep
      )
        .withRef(buttonRef)

    for
      q <- buildQueue
      _ <- withRendered(comp(q)): d =>
             d.innerHTML.assert("(Pending,true)")
             for
               _ <- act(IO.sleep(50.millis))
               _  = d.innerHTML.assert("(Ready(1),false)")
               _ <- act_(Simulate.click(buttonRef.unsafeGet()))       // refresh
               _  = d.innerHTML.assert("(Pending,true)")
               _ <- act(IO.sleep(50.millis))
               _  = d.innerHTML.assert("(Ready(2),false)")
               _ <- act_(Simulate.doubleClick(buttonRef.unsafeGet())) // refreshKeep
               _  = d.innerHTML.assert("(Ready(2),true)")
               _ <- act(IO.sleep(50.millis))
               _  = d.innerHTML.assert("(Ready(3),false)")
             yield ()
    yield ()

  test("useEffectResultWithDeps - call only when deps non-reusable or refresh invoked"):
    val buttonRef = Ref[dom.HTMLButtonElement]

    val comp = ScalaFnComponent[Queue[IO, Int]]: q =>
      for
        s <- useState(MyInt(0))
        v <- useEffectResultWithDeps(s.value): _ =>
               IO.sleep(50.millis) >> q.take
      yield <.button(
        (v.value, v.isRunning).toString,
        ^.onClick --> s.modState(_.inc),
        ^.onDoubleClick --> v.refresh
      ).withRef(buttonRef)

    for
      q <- buildQueue
      _ <- withRendered(comp(q)): d =>
             d.innerHTML.assert("(Pending,true)")
             for
               _ <- act(IO.sleep(50.millis))
               _  = d.innerHTML.assert("(Ready(1),false)")
               _ <- act_(Simulate.click(buttonRef.unsafeGet()))       // force rerender, should reuse
               _  = d.innerHTML.assert("(Ready(1),false)")
               _ <- act_(Simulate.click(buttonRef.unsafeGet()))       // force rerender
               _  = d.innerHTML.assert("(Pending,true)")
               _ <- act(IO.sleep(50.millis))
               _  = d.innerHTML.assert("(Ready(2),false)")
               _ <- act_(Simulate.doubleClick(buttonRef.unsafeGet())) // refresh
               _  = d.innerHTML.assert("(Pending,true)")
               _ <- act(IO.sleep(50.millis))
               _  = d.innerHTML.assert("(Ready(3),false)")
             yield ()
    yield ()

  test("useEffectKeepResultOnMount - only call once"):
    val buttonRef = Ref[dom.HTMLButtonElement]

    val comp = ScalaFnComponent[Queue[IO, Int]]: q =>
      for
        s <- useState(0)
        v <- useEffectKeepResultOnMount(IO.sleep(50.millis) >> q.take)
      yield <.button((v.value, v.isRunning).toString, ^.onClick --> s.modState(_ + 1))
        .withRef(buttonRef)

    for
      q <- buildQueue
      _ <- withRendered(comp(q)): d =>
             d.innerHTML.assert("(Pending,true)")
             for
               _ <- act(IO.sleep(50.millis))
               _  = d.innerHTML.assert("(Ready(1),false)")
               _ <- act_(Simulate.click(buttonRef.unsafeGet())) // force rerender
               _  = d.innerHTML.assert("(Ready(1),false)")
             yield ()
    yield ()

  test("useEffectKeepResultOnMount - call when refresh invoked"):
    val buttonRef = Ref[dom.HTMLButtonElement]

    val comp = ScalaFnComponent[Queue[IO, Int]]: q =>
      for
        s <- useState(0)
        v <- useEffectKeepResultOnMount(IO.sleep(50.millis) >> q.take)
      yield <.button(
        (v.value, v.isRunning).toString,
        ^.onClick --> v.refresh,
        ^.onDoubleClick --> v.refreshNoKeep
      )
        .withRef(buttonRef)

    for
      q <- buildQueue
      _ <- withRendered(comp(q)): d =>
             d.innerHTML.assert("(Pending,true)")
             for
               _ <- act(IO.sleep(50.millis))
               _  = d.innerHTML.assert("(Ready(1),false)")
               _ <- act_(Simulate.click(buttonRef.unsafeGet()))       // refresh
               _  = d.innerHTML.assert("(Ready(1),true)")
               _ <- act(IO.sleep(50.millis))
               _  = d.innerHTML.assert("(Ready(2),false)")
               _ <- act_(Simulate.doubleClick(buttonRef.unsafeGet())) // refreshNoKeep
               _  = d.innerHTML.assert("(Pending,true)")
               _ <- act(IO.sleep(50.millis))
               _  = d.innerHTML.assert("(Ready(3),false)")
             yield ()
    yield ()

  test("useEffectKeepResultWithDeps - call only when deps non-reusable or refresh invoked"):
    val buttonRef = Ref[dom.HTMLButtonElement]

    val comp = ScalaFnComponent[Queue[IO, Int]]: q =>
      for
        s <- useState(MyInt(0))
        v <- useEffectKeepResultWithDeps(s.value): _ =>
               IO.sleep(50.millis) >> q.take
      yield <.button(
        (v.value, v.isRunning).toString,
        ^.onClick --> s.modState(_.inc),
        ^.onDoubleClick --> v.refresh
      ).withRef(buttonRef)

    for
      q <- buildQueue
      _ <- withRendered(comp(q)): d =>
             d.innerHTML.assert("(Pending,true)")
             for
               _ <- act(IO.sleep(50.millis))
               _  = d.innerHTML.assert("(Ready(1),false)")
               _ <- act_(Simulate.click(buttonRef.unsafeGet()))       // force rerender, should reuse
               _  = d.innerHTML.assert("(Ready(1),false)")
               _ <- act_(Simulate.click(buttonRef.unsafeGet()))       // force rerender
               _  = d.innerHTML.assert("(Ready(1),true)")
               _ <- act(IO.sleep(50.millis))
               _  = d.innerHTML.assert("(Ready(2),false)")
               _ <- act_(Simulate.doubleClick(buttonRef.unsafeGet())) // refresh
               _  = d.innerHTML.assert("(Ready(2),true)")
               _ <- act(IO.sleep(50.millis))
               _  = d.innerHTML.assert("(Ready(3),false)")
             yield ()
    yield ()

// useEffectKeepResultWhenDepsReady,
// useEffectKeepResultWhenDepsReadyOrChange,
// useEffectResultWhenDepsReady,
// useEffectResultWhenDepsReadyOrChange,
