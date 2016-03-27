package com.igorvovk.telegram.botapi.examples

import javax.inject.Inject

import akka.actor.{ActorSystem, Props}
import com.igorvovk.telegram.botapi._


class Bootstrap @Inject() (system: ActorSystem, client: TelegramApiClient) {

  val logger = system.actorOf(SimpleMessageLoggerActor(), "message_logger")

  val bot: Props = StatefulBotActor()
  val gate: Props = ChatGateActor(client, ChatGateActorConfig(bot, messageLogger = Some(logger)))

  val emitter = system.actorOf(UpdatesEmitterActor(client), "emitter")
  val router = system.actorOf(PerChatMessageRouterActor(gate), "router")

  emitter ! UpdatesEmitterActor.AddSubscriber(router)
  emitter ! UpdatesEmitterActor.StartReceivingUpdates

}
