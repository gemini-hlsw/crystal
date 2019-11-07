package com.rpiaggio.crystal.effects

import japgolly.scalajs.react.CallbackTo

import cats.syntax.all._
import org.scalacheck.Arbitrary.{arbitrary => getArbitrary}
import org.scalacheck._
import scala.util.Either

object arbitrary {
  def genSyncIO[A: Arbitrary: Cogen]: Gen[CallbackTo[A]] =
    Gen.frequency(5 -> genPure[A], 5 -> genApply[A], /*1 -> genFail[A],*/ 5 -> genBindSuspend[A])

  def genPure[A: Arbitrary]: Gen[CallbackTo[A]] =
    getArbitrary[A].map(CallbackTo.pure)

  def genApply[A: Arbitrary]: Gen[CallbackTo[A]] =
    getArbitrary[A].map(CallbackTo.apply(_))

//  def genFail[A]: Gen[CallbackTo[A]] =
//    getArbitrary[Throwable].map(CallbackTo.raiseError)

  // ApplicativeError!


  def genBindSuspend[A: Arbitrary: Cogen]: Gen[CallbackTo[A]] =
    getArbitrary[A].map(CallbackTo.apply(_).flatMap(CallbackTo.pure))
}
