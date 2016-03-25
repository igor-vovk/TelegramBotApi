package com.igorvovk.telegram.botapi.examples

import akka.actor.{ActorSystem, Props}
import com.google.inject.Inject
import com.igorvovk.telegram.botapi.{PerChatMessageRouter, TelegramApiClient, UpdatesEmitterActor}


class Bootstrap @Inject()(system: ActorSystem, client: TelegramApiClient) {

  val bot: Props = StatefulBotActor.props(client)

  val emitter = system.actorOf(UpdatesEmitterActor.props(client), "emitter")
  val router = system.actorOf(PerChatMessageRouter.props(bot), "router")

  emitter ! UpdatesEmitterActor.AddSubscriber(router)
  emitter ! UpdatesEmitterActor.StartReceivingUpdates

}
