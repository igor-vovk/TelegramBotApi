package com.igorvovk.telegram.botapi

import akka.actor._

import scala.collection.mutable

object PerChatMessageRouterActor {

  def apply(underlying: Props) = Props(classOf[PerChatMessageRouterActor], underlying)

}

/**
  * Spawns child actors per chat
  *
  * @param props Child actor props
  */
class PerChatMessageRouterActor(props: Props) extends Actor with ActorLogging {

  import context._

  private val mem: mutable.Map[Long, ActorRef] = mutable.LongMap.empty

  override def receive: Receive = {
    case msg: Message =>
      val key = msg.chat.id

      val ac = mem.getOrElseUpdate(key, {
        val name = "chat@" + key

        log.debug("Spawning actor {}", name)
        watch(actorOf(props, name))
      })

      ac ! msg
    case Terminated(actor) =>
      log.debug("Terminated actor {}", actor.path)

      mem.find(_._2 == actor).foreach(mem -= _._1)
  }
}
