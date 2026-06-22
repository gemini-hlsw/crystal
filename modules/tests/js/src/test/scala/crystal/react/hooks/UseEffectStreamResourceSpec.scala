// Copyright (c) 2016-2025 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal.react.hooks

import cats.effect.Deferred
import cats.effect.IO
import cats.effect.Resource
import cats.syntax.all.*
import crystal.*
import japgolly.scalajs.react.*
import japgolly.scalajs.react.test.ReactTestUtils
import japgolly.scalajs.react.testing_library.dom.Simulate
import japgolly.scalajs.react.vdom.html_<^.*
import munit.CatsEffectSuite
import org.scalajs.dom

import scala.concurrent.duration.*

// Exercises `useEffectStream` / `useEffectStreamResource` against their README descriptions:
// "Executes and drains a `fs2.Stream[IO, Unit]` upon mount [...] The resource is also closed if the
// stream terminates."
class UseEffectStreamResourceSpec extends CatsEffectSuite:
  import ReactTestUtils.*

  test("useEffectStream - drains the stream on mount"):
    val log     = cats.effect.Ref.unsafe[IO, List[Int]](Nil)
    val drained = Deferred.unsafe[IO, Unit] // signalled when the stream terminates

    val comp = ScalaFnComponent[Unit]: _ =>
      for _ <- useEffectStreamOnMount(
                 fs2.Stream
                   .emits(List(1, 2, 3))
                   .covary[IO]
                   .evalMap(i => log.update(i :: _))
                   // `onComplete` runs on normal termination but not on cancellation, so a transient
                   // mount/cleanup (effects are double-invoked) won't fire this latch early.
                   .onComplete(fs2.Stream.exec(drained.complete(()).void))
               )
      yield EmptyVdom

    withRendered(comp()): _ =>
      for
        _ <- act(drained.get) // wait until the stream has been fully drained
        r <- log.get
        _  = assertEquals(r.reverse, List(1, 2, 3))
      yield ()

  test("useEffectStreamResource - opens the resource, drains the stream, then closes it"):
    val opened  = cats.effect.Ref.unsafe[IO, Boolean](false)
    val log     = cats.effect.Ref.unsafe[IO, List[Int]](Nil)
    val drained = Deferred.unsafe[IO, Unit] // signalled when the stream terminates (not on cancel)

    val comp = ScalaFnComponent[Unit]: _ =>
      for _ <- useEffectStreamResourceOnMount(
                 Resource
                   .make(opened.set(true))(_ => opened.set(false))
                   .as(
                     fs2.Stream
                       .emits(List(1, 2))
                       .covary[IO]
                       .evalMap(i => log.update(i :: _))
                       .onComplete(fs2.Stream.exec(drained.complete(()).void))
                   )
               )
      yield EmptyVdom

    withRendered(comp()): _ =>
      for
        _ <-
          act(drained.get) // stream fully drained (the resource close can't be latched, see below)
        r <- log.get
        _  = assertEquals(r.reverse, List(1, 2)) // stream drained
        // The resource closes *after* the stream terminates. A latch on the release would fire on the
        // transient mount/cleanup (effects are double-invoked), so we wait with a generous margin.
        _ <- act(IO.sleep(200.millis))
        o <- opened.get
        _  = assertEquals(o, false)              // resource closed after the stream terminated
      yield ()

  test("useEffectStreamWhenDepsReady - drains the stream only once the dependency is Ready"):
    val log       = cats.effect.Ref.unsafe[IO, List[Int]](Nil)
    val drained   = Deferred.unsafe[IO, Unit]
    val buttonRef = Ref[dom.HTMLButtonElement]

    val comp = ScalaFnComponent[Unit]: _ =>
      for
        dep <- useState(Pot.pending[Unit])
        _   <- useEffectStreamWhenDepsReady(dep.value): _ =>
                 fs2.Stream
                   .emits(List(1, 2, 3))
                   .covary[IO]
                   .evalMap(i => log.update(i :: _))
                   .onComplete(fs2.Stream.exec(drained.complete(()).void))
      yield <.button("x", ^.onClick --> dep.setState(().ready)).withRef(buttonRef)

    withRendered(comp()): _ =>
      for
        pre <- log.get
        _    = assertEquals(pre, Nil)                      // not drained while the dependency is Pending
        _   <- act_(Simulate.click(buttonRef.unsafeGet())) // dependency becomes Ready -> drain
        _   <- act(drained.get)
        r   <- log.get
        _    = assertEquals(r.reverse, List(1, 2, 3))
      yield ()
