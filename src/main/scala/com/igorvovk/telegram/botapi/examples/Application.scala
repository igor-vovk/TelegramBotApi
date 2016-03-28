package com.igorvovk.telegram.botapi.examples

import akka.actor.Props
import com.igorvovk.telegram.botapi.{TelegramApiClientConfiguration, TelegramApiClientConfigurationProvider}
import play.api.inject._
import play.api.inject.guice.GuiceApplicationBuilder


object Application extends App {

  GuiceApplicationBuilder()
    .bindings(
      bind[TelegramApiClientConfiguration].toProvider[TelegramApiClientConfigurationProvider],
      bind[RunnerConfiguration].toProvider[RunnerConfigurationProvider],
      bind[Props].qualifiedWith("bot_from_config").toProvider[RunnerBotProvider],
      bind[Bootstrap].toSelf.eagerly()
    )
    .build()

}
