// Copyright (c) 2016-2025 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal.react.syntax

import crystal.*
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*

trait pot {
  extension [A](pot: Pot[A])
    def renderPending(f: => VdomNode): VdomNode =
      pot match
        case Pot.Pending => f
        case _           => EmptyVdom

    def renderError(f: Throwable => VdomNode): VdomNode =
      pot match
        case Pot.Error(t) => f(t)
        case _            => EmptyVdom

    def renderReady(f: A => VdomNode): VdomNode =
      pot match
        case Pot.Ready(a) => f(a)
        case _            => EmptyVdom

  extension [A](self: Reusable[Pot[A]])
    def sequencePot[B >: A]: Pot[Reusable[B]] =
      self.value.map(self.withValue(_))

  given Reusability[Throwable] = Reusability.byRef[Throwable]

  given [A: Reusability]: Reusability[Pot[A]] =
    Reusability((x, y) =>
      x match
        case Pot.Pending   =>
          y match
            case Pot.Pending => true
            case _           => false
        case Pot.Error(tx) =>
          y match
            case Pot.Error(ty) => tx ~=~ ty
            case _             => false
        case Pot.Ready(ax) =>
          y match
            case Pot.Ready(ay) => ax ~=~ ay
            case _             => false
    )

  given [A: Reusability]: Reusability[PotOption[A]] =
    Reusability((x, y) =>
      x match
        case PotOption.Pending       =>
          y match
            case PotOption.Pending => true
            case _                 => false
        case PotOption.Error(tx)     =>
          y match
            case PotOption.Error(ty) => tx ~=~ ty
            case _                   => false
        case PotOption.ReadyNone     =>
          y match
            case PotOption.ReadyNone => true
            case _                   => false
        case PotOption.ReadySome(ax) =>
          y match
            case PotOption.ReadySome(ay) => ax ~=~ ay
            case _                       => false
    )
}

object pot extends pot
