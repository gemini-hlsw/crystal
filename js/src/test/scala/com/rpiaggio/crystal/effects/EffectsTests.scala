package com.rpiaggio.crystal.effects

import cats.effect.SyncIO
import cats.effect.laws.discipline.SyncTests
import japgolly.scalajs.react.CallbackTo
import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers

import cats.effect.internals.IOPlatform
import cats.effect.laws.discipline.SyncTests
import cats.effect.laws.discipline.arbitrary._
import cats.implicits._
import cats.kernel.laws.discipline.MonoidTests
import cats.laws._
import cats.laws.discipline._

class EffectsTests /*extends BaseTestsSuite*/ {
  implicit val s: cats.effect.Sync[CallbackTo] = ???

//  checkAllAsync("CallbackTo", _ => SyncTests[CallbackTo].sync[Int, Int, Int])
}
