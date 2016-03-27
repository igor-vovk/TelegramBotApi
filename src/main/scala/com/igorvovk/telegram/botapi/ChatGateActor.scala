package com.igorvovk.telegram.botapi

import akka.actor.{Actor, ActorLogging, ActorRef, Cancellable, PoisonPill, Props, Status, Terminated}
import akka.pattern.pipe

import scala.concurrent.duration._
import scala.language.postfixOps

case class ChatGateActorConfig(underlying: Props,
                               sleepDelay: Duration = 30 seconds,
                               simplifyMessagePassing: Boolean = false,
                               messageLogger: Option[ActorRef] = None)

object ChatGateActor {

  def apply(client: TelegramApiClient, underlying: Props): Props = apply(client, ChatGateActorConfig(underlying))

  def apply(client: TelegramApiClient, config: ChatGateActorConfig): Props = {
    Props(classOf[ChatGateActor], client, config)
  }

}

class ChatGateActor(client: TelegramApiClient, config: ChatGateActorConfig) extends Actor with ActorLogging {

  import context._

  val bot: ActorRef = watch(actorOf(config.underlying, "bot"))
  var shutdownTimer: Option[Cancellable] = None

  def receive = {
    case m: Message =>
      become(configured(m.chat))

      processMessage(m)
  }

  def configured(chat: Chat): Receive = {
    // Messages from router
    case m: Message =>
      processMessage(m)

    // Messages from bot
    case s: OutMessage =>
      client.send(chat.sid, s).pipeTo(self)
    case s: SendChatAction =>
      client.sendChatAction(chat.sid, s)

    // System messages
    case f@Status.Failure(e) =>
      log.error(e, "Error in API Client")

      bot forward f
    case Terminated(some) =>
      if (some == bot) {
        stop(self)
      }
  }


  override def postStop(): Unit = {
    shutdownTimer.foreach(_.cancel())
  }

  def processMessage(m: Message) = {
    config.messageLogger.foreach(_ ! m)

    if (!messageFromSelf(m)) {
      if (config.simplifyMessagePassing) {
        m.data.foreach(bot ! _)
      } else {
        bot ! m
      }
    }

    scheduleShutdown()
  }

  def messageFromSelf(m: Message): Boolean = {
    m.from.map(_.id).contains(client.botId)
  }

  def scheduleShutdown(): Unit = {
    config.sleepDelay match {
      case f: FiniteDuration =>
        shutdownTimer.foreach(_.cancel())
        shutdownTimer = Some(system.scheduler.scheduleOnce(f, bot, PoisonPill))
      case _ => // Do nothing
    }
  }


}
