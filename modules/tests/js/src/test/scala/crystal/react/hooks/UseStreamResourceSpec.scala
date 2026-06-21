// Copyright (c) 2016-2025 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal.react.hooks

import cats.effect.Deferred
import cats.effect.IO
import cats.effect.Resource
import cats.effect.std.Queue
import cats.syntax.all.*
import japgolly.scalajs.react.*
import japgolly.scalajs.react.test.ReactTestUtils
import japgolly.scalajs.react.testing_library.dom.Simulate
import japgolly.scalajs.react.vdom.html_<^.*
import munit.CatsEffectSuite
import org.scalajs.dom

// Exercises the `useStream*` family against its README description: the value is provided as a
// `PotOption` (`Pending` before start, `ReadyNone` once started with no value yet, `ReadySome(a)`
// for the last produced value), and `*View` variants allow local modification.
//
// The stream is driven from a test-controlled `Queue`, and an `evalTap` offers each element to a
// second queue, so the test steps one value at a time and waits on a latch — no timed sleeps, hence
// no dependency on CI scheduling.
class UseStreamResourceSpec extends CatsEffectSuite:
  import ReactTestUtils.*

  test("useStream - reflects the latest produced value as a PotOption"):
    for
      control   <- Queue.unbounded[IO, Int]
      processed <- Queue.unbounded[IO, Int]
      started   <- Deferred[IO, Unit]
      comp       = ScalaFnComponent[Unit]: _ =>
                     for v <- useStreamOnMount(
                                fs2.Stream.exec(started.complete(()).void) ++
                                  fs2.Stream.fromQueueUnterminated(control).evalTap(processed.offer)
                              )
                     yield <.div(v.toString)
      _         <- withRendered(comp()): d =>
                     for
                       _ <- act(started.get)    // stream fiber has started
                       _  = d.innerHTML.assert("ReadyNone")
                       _ <- control.offer(1)
                       _ <- act(processed.take) // value 1 produced and rendered
                       _  = d.innerHTML.assert("ReadySome(1)")
                       _ <- control.offer(2)
                       _ <- act(processed.take)
                       _  = d.innerHTML.assert("ReadySome(2)")
                       _ <- control.offer(3)
                       _ <- act(processed.take)
                       _  = d.innerHTML.assert("ReadySome(3)")
                     yield ()
    yield ()

  test("useStreamResource - opens the resource and reflects produced values"):
    val opened = cats.effect.Ref.unsafe[IO, Boolean](false)

    for
      control   <- Queue.unbounded[IO, Int]
      processed <- Queue.unbounded[IO, Int]
      comp       = ScalaFnComponent[Unit]: _ =>
                     for v <- useStreamResourceOnMount(
                                Resource
                                  .make(opened.set(true))(_ => opened.set(false))
                                  .as(fs2.Stream.fromQueueUnterminated(control).evalTap(processed.offer))
                              )
                     yield <.div(v.toString)
      _         <- withRendered(comp()): d =>
                     for
                       _ <- control.offer(7)
                       _ <- act(processed.take)
                       _  = d.innerHTML.assert("ReadySome(7)")
                       o <- opened.get
                       _  = assertEquals(o, true) // resource was acquired
                       _ <- control.offer(8)
                       _ <- act(processed.take)
                       _  = d.innerHTML.assert("ReadySome(8)")
                     yield ()
    yield ()

  test("useStreamView - exposes the produced value as a locally modifiable View"):
    val buttonRef = Ref[dom.HTMLButtonElement]

    for
      control   <- Queue.unbounded[IO, Int]
      processed <- Queue.unbounded[IO, Int]
      comp       = ScalaFnComponent[Unit]: _ =>
                     for v <- useStreamViewOnMount(
                                fs2.Stream.fromQueueUnterminated(control).evalTap(processed.offer)
                              )
                     yield <.button(
                       v.toOption.map(_.get).toString,
                       ^.onClick --> v.toOption.map(_.set(99)).getOrElse(Callback.empty)
                     ).withRef(buttonRef)
      _         <- withRendered(comp()): d =>
                     for
                       _ <- control.offer(1)
                       _ <- act(processed.take)
                       _  = d.innerHTML.assert("Some(1)")  // value from the stream
                       _ <- act_(Simulate.click(buttonRef.unsafeGet()))
                       _  = d.innerHTML.assert("Some(99)") // locally modified through the View
                     yield ()
    yield ()

  test("useStream - resubscribes to a new stream when the dependency changes"):
    val buttonRef = Ref[dom.HTMLButtonElement]

    for
      controlA  <- Queue.unbounded[IO, Int]
      controlB  <- Queue.unbounded[IO, Int]
      processed <- Queue.unbounded[IO, Int]
      comp       = ScalaFnComponent[Unit]: _ =>
                     for
                       dep <- useState(0)
                       v   <- useStream(dep.value): d =>
                                fs2.Stream
                                  .fromQueueUnterminated(if d == 0 then controlA else controlB)
                                  .evalTap(processed.offer)
                     yield <.button(v.toString, ^.onClick --> dep.modState(_ + 1)).withRef(buttonRef)
      _         <- withRendered(comp()): d =>
                     for
                       _ <- controlA.offer(1)
                       _ <- act(processed.take)
                       _  = d.innerHTML.assert("ReadySome(1)")
                       _ <- act_(Simulate.click(buttonRef.unsafeGet())) // dep change -> resubscribe
                       _ <- controlB.offer(7)
                       _ <- act(processed.take)
                       _  = d.innerHTML.assert("ReadySome(7)")          // values now come from the new stream
                     yield ()
    yield ()

  test("useStreamResourceView - produced values are a locally modifiable View"):
    val buttonRef = Ref[dom.HTMLButtonElement]

    for
      control   <- Queue.unbounded[IO, Int]
      processed <- Queue.unbounded[IO, Int]
      comp       = ScalaFnComponent[Unit]: _ =>
                     for v <- useStreamResourceViewOnMount(
                                Resource.pure(
                                  fs2.Stream.fromQueueUnterminated(control).evalTap(processed.offer)
                                )
                              )
                     yield <.button(
                       v.toOption.map(_.get).toString,
                       ^.onClick --> v.toOption.map(_.set(99)).getOrElse(Callback.empty)
                     ).withRef(buttonRef)
      _         <- withRendered(comp()): d =>
                     for
                       _ <- control.offer(5)
                       _ <- act(processed.take)
                       _  = d.innerHTML.assert("Some(5)")
                       _ <- act_(Simulate.click(buttonRef.unsafeGet()))
                       _  = d.innerHTML.assert("Some(99)")
                     yield ()
    yield ()

  test("useStreamViewWithReuse - exposes a reusable PotOption View of produced values"):
    for
      control   <- Queue.unbounded[IO, Int]
      processed <- Queue.unbounded[IO, Int]
      comp       = ScalaFnComponent[Unit]: _ =>
                     for v <- useStreamViewWithReuseOnMount(
                                fs2.Stream.fromQueueUnterminated(control).evalTap(processed.offer)
                              )
                     yield <.div(v.value.toOption.map(_.get).toString)
      _         <- withRendered(comp()): d =>
                     for
                       _ <- control.offer(3)
                       _ <- act(processed.take)
                       _  = d.innerHTML.assert("Some(3)")
                     yield ()
    yield ()
