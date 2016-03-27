package com.igorvovk.telegram.botapi.examples

import akka.actor.{Actor, Props}
import com.igorvovk.telegram.botapi.{Message, SendMessage}

object HelloWorldActor {

  def apply(): Props = Props(classOf[HelloWorldActor])

}

class HelloWorldActor extends Actor {

  def receive = {
    case m: Message =>
      sender() ! SendMessage("Hello world!")
  }

}
