package com.igorvovk.telegram.botapi.examples

import javax.inject.{Inject, Named, Provider, Singleton}

import akka.actor.{ActorSystem, Props}
import com.igorvovk.telegram.botapi._
import play.api.Configuration
import play.api.inject.Injector

@Singleton
class RunnerConfigurationProvider @Inject() (config: Configuration) extends Provider[RunnerConfiguration] {
  lazy val get = {
    val option = for {
      ns <- config.getConfig("telegram.runner")
      providerClass <- ns.getString("providerClass")
    } yield {
      RunnerConfiguration(
        providerClass = providerClass
      )
    }

    option.get
  }
}

case class RunnerConfiguration(providerClass: String)

class RunnerBotProvider @Inject() (runnerConfiguration: RunnerConfiguration, injector: Injector) extends Provider[Props] {
  def get() = {
    injector.instanceOf(getClass(runnerConfiguration.providerClass, classOf[Provider[Props]])).get()
  }

  private def getClass[T](className: String, instanceOf: Class[T]) = {
    Class.forName(className).asSubclass(instanceOf)
  }
}

class Bootstrap @Inject() (system: ActorSystem, client: TelegramApiClient, @Named("bot_from_config") bot: Props) {
  val logger = system.actorOf(SimpleMessageLoggerActor(), "message_logger")

  val gate: Props = ChatGateActor(client, ChatGateActorConfig(bot, messageLogger = Some(logger)))

  val emitter = system.actorOf(UpdatesEmitterActor(client), "emitter")
  val router = system.actorOf(PerChatMessageRouterActor(gate), "router")

  emitter ! UpdatesEmitterActor.AddSubscriber(router)
  emitter ! UpdatesEmitterActor.StartReceivingUpdates

}
