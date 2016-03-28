package com.igorvovk.telegram.botapi.examples

import javax.inject.{Provider, Singleton}

import akka.actor.{Actor, Props}
import com.igorvovk.telegram.botapi.{Message, SendMessage}

@Singleton
class HelloWorldBotProvider extends Provider[Props] {
  lazy val get = Props(classOf[HelloWorldActor])
}

class HelloWorldActor extends Actor {

  def receive = {
    case m: Message =>
      sender() ! SendMessage("Hello world!")
  }

}
