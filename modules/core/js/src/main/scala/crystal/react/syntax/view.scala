// Copyright (c) 2016-2025 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal.react.syntax

import cats.Monad
import cats.effect.Async
import cats.syntax.all.*
import crystal.*
import crystal.react.*
import crystal.react.reuse.*
import japgolly.scalajs.react.*
import japgolly.scalajs.react.util.DefaultEffects.Async as DefaultA
import org.typelevel.log4cats.Logger

import scala.reflect.ClassTag

trait view {
  extension [A](view: View[A])
    def async(using Logger[DefaultA]): ViewF[DefaultA, A] =
      view.to[DefaultA](syncToAsync.apply[Unit], _.runAsync)

  extension [F[_], A: ClassTag: Reusability](view: ViewF[F, A])
    def reuseByValue: Reuse[ViewF[F, A]] = Reuse.by(view.get)(view)

  extension (viewFModule: ViewF.type) def fromState: FromStateView = new FromStateView

  extension [F[_], A: Reusability](view: ViewOptF[F, A])
    def reuseByValue: Reuse[ViewOptF[F, A]] = Reuse.by(view.get)(view)

  extension [F[_]: Monad, A](optView: Option[ViewF[F, A]])
    def toViewOpt: ViewOptF[F, A] =
      optView.fold(new ViewOptF[F, A](none, (_, cb) => cb(none, none)) {
        override def modAndGet(f: A => A)(using F: Async[F]): F[Option[A]] = none.pure[F]
      })(_.asViewOpt)

  extension [A](view: ReuseView[A])
    def async(using Logger[DefaultA]): ReuseViewF[DefaultA, A] =
      view.map(_.to[DefaultA](syncToAsync.apply[Unit], _.runAsync))
}

object view extends view
