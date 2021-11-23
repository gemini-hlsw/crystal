package crystal.react.hooks

import japgolly.scalajs.react.Reusability
import monocle.Lens

case class SerialState[A] private (
  protected[hooks] val value:  A,
  protected[hooks] val serial: Int = Int.MinValue
) {
  def update(f: A => A): SerialState[A] = SerialState(f(value), serial + 1)
}

object SerialState {
  def initial[A](value: A): SerialState[A] = SerialState(value)

  def value[A]: Lens[SerialState[A], A] = Lens[SerialState[A], A](_.value)(v => _.update(_ => v))

  implicit def reuseSerialState[A]: Reusability[SerialState[A]] = Reusability.by(_.serial)
}
