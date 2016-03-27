package com.igorvovk.telegram.botapi

import akka.actor.{Actor, ActorLogging, Props}

object SimpleMessageLoggerActor {

  def apply(): Props = Props(classOf[SimpleMessageLoggerActor])

}

class SimpleMessageLoggerActor extends Actor with ActorLogging {
  def receive = {
    case m: Message => log.info("Received new message: {}", m)
  }
}
