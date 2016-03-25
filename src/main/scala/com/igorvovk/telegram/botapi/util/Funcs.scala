package com.igorvovk.telegram.botapi.util

trait Funcs {

  def mapO2P[T](first: String, second: Option[T]): Option[(String, T)] = second.map(first -> _)

}

object Funcs extends Funcs
