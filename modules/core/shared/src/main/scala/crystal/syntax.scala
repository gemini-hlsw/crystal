// Copyright (c) 2016-2023 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package crystal

import scala.annotation.targetName
import scala.util.Try

trait syntax:
  def pending[A]: Pot[A]          = Pot.Pending
  def pendingOpt[A]: PotOption[A] = PotOption.Pending

  extension [A](a: A)
    def ready: Pot[A]           = Pot.Ready(a)
    def readySome: PotOption[A] = PotOption.ReadySome(a)

  extension [A](a: Option[A])
    @targetName("optionToPot")
    def toPot: Pot[A]             = Pot.fromOption(a)
    @targetName("optionToPotOption")
    def toPotOption: PotOption[A] = PotOption.fromOption(a)

  extension [A](a: Option[Try[A]])
    @targetName("optionTryToPot")
    def toPot: Pot[A]             = Pot.fromOptionTry(a)
    @targetName("optionTryToPotOption")
    def toPotOption: PotOption[A] = PotOption.fromOptionTry(a)

  extension [A](a: Try[A])
    @targetName("tryToPot")
    def toPot: Pot[A]             = Pot.fromTry[A](a)
    @targetName("tryToPotOption")
    def toPotOption: PotOption[A] = PotOption.fromTry(a)

object syntax extends syntax
