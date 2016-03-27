package com.igorvovk.telegram.botapi.examples

import com.igorvovk.telegram.botapi.{TelegramApiClientConfiguration, TelegramApiClientConfigurationProvider}
import play.api.inject._
import play.api.inject.guice.GuiceApplicationBuilder


object Application extends App {

  GuiceApplicationBuilder()
    .bindings(
      bind[TelegramApiClientConfiguration].toProvider[TelegramApiClientConfigurationProvider],
      bind[Bootstrap].toSelf.eagerly()
    )
    .build()

}
