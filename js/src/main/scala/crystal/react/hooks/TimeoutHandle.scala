package crystal.react.hooks

import cats.effect.Fiber
import cats.effect.Temporal
import cats.syntax.all._
import crystal.react.implicits._
import japgolly.scalajs.react.hooks.Hooks
import japgolly.scalajs.react.util.DefaultEffects.{ Async => DefaultA }
import japgolly.scalajs.react.util.DefaultEffects.{ Sync => DefaultS }

import scala.concurrent.duration.FiniteDuration

class TimeoutHandle(
  duration:          FiniteDuration,
  timerRef:          Hooks.UseRefF[DefaultS, Option[Fiber[DefaultA, Throwable, Unit]]]
)(implicit DefaultA: Temporal[DefaultA]) {
  private val cleanup: DefaultA[Unit] = timerRef.set(none).to[DefaultA]

  val cancel: DefaultA[Unit] =
    DefaultA.pure(timerRef.value) >>= (runningFiber =>
      (cleanup >> runningFiber.map(_.cancel).orEmpty).uncancelable
    )

  def submit(effect: DefaultA[Unit]): DefaultA[Unit] = {
    val timedEffect =
      // We don't cleanup until `effect` completes, so that we can still invoke `cancel` when the effect is running.
      DefaultA.sleep(duration) >> effect.guarantee(cleanup.uncancelable)
    cancel >>
      (timedEffect.start >>=
        (fiber => timerRef.set(fiber.some).to[DefaultA])).uncancelable
  }
}
