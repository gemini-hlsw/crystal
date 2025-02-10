// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal.react.hooks

import cats.effect.IO
import cats.effect.std.Queue
import cats.syntax.all.*
import crystal.Pot
import crystal.syntax.*
import japgolly.scalajs.react.*
import japgolly.scalajs.react.test.ReactTestUtils2
import japgolly.scalajs.react.test.Simulate
import japgolly.scalajs.react.vdom.html_<^.*
import munit.CatsEffectSuite
import org.scalajs.dom

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

  test("useEffectResultWhenDepsReady - call every time deps become ready"):
    val buttonRef = Ref[dom.HTMLButtonElement]

    val comp = ScalaFnComponent[Queue[IO, Int]]: q =>
      for
        s <- useState(Pot.pending[Int])
        v <- useEffectResultWhenDepsReady(s.value): _ =>
               IO.sleep(50.millis) >> q.take
      yield <.button(
        (v.value, v.isRunning).toString,
        ^.onClick --> s.modState(_.fold(0, _ => 0, _ + 1).ready),
        ^.onDoubleClick --> s.setState(Pot.error(RuntimeException("error")))
      ).withRef(buttonRef)

    for
      q <- buildQueue
      _ <- withRendered(comp(q)): d =>
             d.innerHTML.assert("(Pending,false)")
             for
               _ <- act(IO.sleep(50.millis))
               _  = d.innerHTML.assert("(Pending,false)")             // Still pending
               _ <- act_(Simulate.click(buttonRef.unsafeGet()))       // Change to ready
               _  = d.innerHTML.assert("(Pending,true)")
               _ <- act(IO.sleep(50.millis))
               _  = d.innerHTML.assert("(Ready(1),false)")
               _ <- act_(Simulate.click(buttonRef.unsafeGet()))       // Increment, don't change value
               _  = d.innerHTML.assert("(Ready(1),false)")
               _ <- act_(Simulate.doubleClick(buttonRef.unsafeGet())) // Change to error
               _  = d.innerHTML.assert("(Ready(1),false)")            // No change in value
               _ <- act_(Simulate.click(buttonRef.unsafeGet()))       // Change to ready again
               _  = d.innerHTML.assert("(Pending,true)")
               _ <- act(IO.sleep(50.millis))
               _  = d.innerHTML.assert("(Ready(2),false)")
             yield ()
    yield ()

  test("useEffectResultWhenDepsReadyOrChange - call every time deps become ready or change"):
    val buttonRef = Ref[dom.HTMLButtonElement]

    val comp = ScalaFnComponent[Queue[IO, Int]]: q =>
      for
        s <- useState(Pot.pending[MyInt])
        v <- useEffectResultWhenDepsReadyOrChange(s.value): _ =>
               IO.sleep(50.millis) >> q.take
      yield <.button(
        (v.value, v.isRunning).toString,
        ^.onClick --> s.modState(_.fold(MyInt(0), _ => MyInt(0), _.inc).ready),
        ^.onDoubleClick --> s.setState(Pot.error(RuntimeException("error")))
      ).withRef(buttonRef)

    for
      q <- buildQueue
      _ <- withRendered(comp(q)): d =>
             d.innerHTML.assert("(Pending,false)")
             for
               _ <- act(IO.sleep(50.millis))
               _  = d.innerHTML.assert("(Pending,false)")             // Still pending
               _ <- act_(Simulate.click(buttonRef.unsafeGet()))       // Change to ready
               _  = d.innerHTML.assert("(Pending,true)")
               _ <- act(IO.sleep(50.millis))
               _  = d.innerHTML.assert("(Ready(1),false)")
               _ <- act_(Simulate.click(buttonRef.unsafeGet()))       // Increment, don't change value
               _  = d.innerHTML.assert("(Ready(1),false)")
               _ <- act_(Simulate.click(buttonRef.unsafeGet()))       // Increment again, should change
               _  = d.innerHTML.assert("(Pending,true)")
               _ <- act(IO.sleep(50.millis))
               _  = d.innerHTML.assert("(Ready(2),false)")
               _ <- act_(Simulate.doubleClick(buttonRef.unsafeGet())) // Change to error
               _  = d.innerHTML.assert("(Ready(2),false)")            // No change in value
               _ <- act_(Simulate.click(buttonRef.unsafeGet()))       // Change to ready again
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

  test("useEffectKeepResultWhenDepsReady - call every time deps become ready"):
    val buttonRef = Ref[dom.HTMLButtonElement]

    val comp = ScalaFnComponent[Queue[IO, Int]]: q =>
      for
        s <- useState(Pot.pending[Int])
        v <- useEffectKeepResultWhenDepsReady(s.value): _ =>
               IO.sleep(50.millis) >> q.take
      yield <.button(
        (v.value, v.isRunning).toString,
        ^.onClick --> s.modState(_.fold(0, _ => 0, _ + 1).ready),
        ^.onDoubleClick --> s.setState(Pot.error(RuntimeException("error")))
      ).withRef(buttonRef)

    for
      q <- buildQueue
      _ <- withRendered(comp(q)): d =>
             d.innerHTML.assert("(Pending,false)")
             for
               _ <- act(IO.sleep(50.millis))
               _  = d.innerHTML.assert("(Pending,false)")             // Still pending
               _ <- act_(Simulate.click(buttonRef.unsafeGet()))       // Change to ready
               _  = d.innerHTML.assert("(Pending,true)")
               _ <- act(IO.sleep(50.millis))
               _  = d.innerHTML.assert("(Ready(1),false)")
               _ <- act_(Simulate.click(buttonRef.unsafeGet()))       // Increment, don't change value
               _  = d.innerHTML.assert("(Ready(1),false)")
               _ <- act_(Simulate.doubleClick(buttonRef.unsafeGet())) // Change to error
               _  = d.innerHTML.assert("(Ready(1),false)")            // No change in value
               _ <- act_(Simulate.click(buttonRef.unsafeGet()))       // Change to ready again
               _  = d.innerHTML.assert("(Ready(1),true)")
               _ <- act(IO.sleep(50.millis))
               _  = d.innerHTML.assert("(Ready(2),false)")
             yield ()
    yield ()

  test("useEffectKeepResultWhenDepsReadyOrChange - call every time deps become ready or change"):
    val buttonRef = Ref[dom.HTMLButtonElement]

    val comp = ScalaFnComponent[Queue[IO, Int]]: q =>
      for
        s <- useState(Pot.pending[MyInt])
        v <- useEffectKeepResultWhenDepsReadyOrChange(s.value): _ =>
               IO.sleep(50.millis) >> q.take
      yield <.button(
        (v.value, v.isRunning).toString,
        ^.onClick --> s.modState(_.fold(MyInt(0), _ => MyInt(0), _.inc).ready),
        ^.onDoubleClick --> s.setState(Pot.error(RuntimeException("error")))
      ).withRef(buttonRef)

    for
      q <- buildQueue
      _ <- withRendered(comp(q)): d =>
             d.innerHTML.assert("(Pending,false)")
             for
               _ <- act(IO.sleep(50.millis))
               _  = d.innerHTML.assert("(Pending,false)")             // Still pending
               _ <- act_(Simulate.click(buttonRef.unsafeGet()))       // Change to ready
               _  = d.innerHTML.assert("(Pending,true)")
               _ <- act(IO.sleep(50.millis))
               _  = d.innerHTML.assert("(Ready(1),false)")
               _ <- act_(Simulate.click(buttonRef.unsafeGet()))       // Increment, don't change value
               _  = d.innerHTML.assert("(Ready(1),false)")
               _ <- act_(Simulate.click(buttonRef.unsafeGet()))       // Increment again, should change
               _  = d.innerHTML.assert("(Ready(1),true)")
               _ <- act(IO.sleep(50.millis))
               _  = d.innerHTML.assert("(Ready(2),false)")
               _ <- act_(Simulate.doubleClick(buttonRef.unsafeGet())) // Change to error
               _  = d.innerHTML.assert("(Ready(2),false)")            // No change in value
               _ <- act_(Simulate.click(buttonRef.unsafeGet()))       // Change to ready again
               _  = d.innerHTML.assert("(Ready(2),true)")
               _ <- act(IO.sleep(50.millis))
               _  = d.innerHTML.assert("(Ready(3),false)")
             yield ()
    yield ()
