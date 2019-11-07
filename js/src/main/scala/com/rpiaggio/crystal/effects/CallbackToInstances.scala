package com.rpiaggio.crystal.effects

import cats.effect.Sync
import japgolly.scalajs.react.CallbackTo

trait CallbackToInstances {

  implicit def callbackToSync: Sync[CallbackTo] = ???
}
