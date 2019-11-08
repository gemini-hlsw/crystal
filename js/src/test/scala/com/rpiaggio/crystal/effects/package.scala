package com.rpiaggio.crystal

import japgolly.scalajs.react.CallbackTo
import org.scalacheck.{Arbitrary, Cogen, Gen}
import org.scalacheck.Arbitrary.arbitrary

package object effects {
//  def genSyncIO[A: Arbitrary: Cogen]: Gen[CallbackTo[A]] =
//    Gen.frequency(5 -> genPure[A], 5 -> genApply[A], /*1 -> genFail[A],*/ 5 -> genBindSuspend[A])

  def genPure[A: Arbitrary]: Gen[CallbackTo[A]] =
    arbitrary[A].map(CallbackTo.pure)

  def genApply[A: Arbitrary]: Gen[CallbackTo[A]] =
    arbitrary[A].map(CallbackTo.apply(_))

  //  def genFail[A]: Gen[CallbackTo[A]] =
  //    getArbitrary[Throwable].map(CallbackTo.raiseError)

  // ApplicativeError!


  def genBindSuspend[A: Arbitrary: Cogen]: Gen[CallbackTo[A]] =
    arbitrary[A].map(CallbackTo.apply(_).flatMap(CallbackTo.pure))

}
