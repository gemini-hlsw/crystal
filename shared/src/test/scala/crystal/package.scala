// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

import cats.Eq
import monocle.Iso
import monocle.Lens
import monocle.Optional
import monocle.Traversal
import monocle.macros.GenLens
import monocle.std.option.some

package crystal {
  case class Wrap[A](a: A) {
    def map[B](f: A => B): Wrap[B] = Wrap(f(a))
  }
  object Wrap              {
    def a[A]: Lens[Wrap[A], A] = GenLens[Wrap[A]](_.a)

    implicit def eqWrap[A: Eq]: Eq[Wrap[A]] = Eq.by(_.a)

    def iso[A]: Iso[Wrap[A], A] = Iso[Wrap[A], A](_.a)(Wrap.apply)
  }

  case class WrapOpt[A](a: Option[A])
  object WrapOpt {
    def a[A]: Lens[WrapOpt[A], Option[A]] = GenLens[WrapOpt[A]](_.a)

    def aOpt[A]: Optional[WrapOpt[A], A] =
      WrapOpt.a.andThen(some[A].asOptional)

    implicit def eqWrapOpt[A: Eq]: Eq[WrapOpt[A]] = Eq.by(_.a)
  }

  case class WrapList[A](a: List[A])
  object WrapList {
    def a[A]: Lens[WrapList[A], List[A]] = GenLens[WrapList[A]](_.a)

    def aList[A]: Traversal[WrapList[A], A] =
      WrapList.a.andThen(Traversal.fromTraverse[List, A])

    implicit def eqWrapList[A: Eq]: Eq[WrapList[A]] = Eq.by(_.a)
  }
}
