// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal.react.hooks

import japgolly.scalajs.react.Reusability

case class SerialState[A] private (
  protected[hooks] val value:  A,
  protected[hooks] val serial: Int = Int.MinValue
) {
  def update(f: A => A): SerialState[A] = SerialState(f(value), serial + 1)
}

object SerialState {
  def initial[A](value: A): SerialState[A] = SerialState(value)

  implicit def reuseSerialState[A]: Reusability[SerialState[A]] = Reusability.by(_.serial)
}
