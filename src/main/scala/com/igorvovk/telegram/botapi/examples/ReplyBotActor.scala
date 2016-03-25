package com.igorvovk.telegram.botapi.examples

import akka.actor.{Actor, Props}
import com.igorvovk.telegram.botapi.{Chat, Message, TelegramApiClient}

object ReplyBotActor {

  def props(client: TelegramApiClient): Props = Props(classOf[ReplyBotActor], client)

}

class ReplyBotActor(client: TelegramApiClient) extends Actor {

  import context._

  var chat: Chat = _

  def receive: Receive = {
    case m: Message =>
      if (null == chat) {
        client.sendMessage(m.chat.id.toString, "New actor spawned")
      }

      chat = m.chat

      client.sendMessage(chat.id.toString, "Hello world!")
  }

  @throws[Exception](classOf[Exception])
  override def postStop(): Unit = {
    client.sendMessage(chat.id.toString, "Bye!")
  }
}
