package com.igorvovk.telegram.botapi

import javax.inject.{Inject, Provider, Singleton}

import com.typesafe.config.Config

object TelegramApiClientConfiguration {

  def fromConfig(config: Config): TelegramApiClientConfiguration = {
    TelegramApiClientConfiguration(
      apiBaseUrl = config.getString("apiUrl") + "/bot" + config.getString("bot.token"),
      downloadBaseUrl = config.getString("apiUrl") + "/file/bot" + config.getString("bot.token"),
      receiveUpdatesTimeout = Some(90)
    )
  }

}

case class TelegramApiClientConfiguration(apiBaseUrl: String,
                                          downloadBaseUrl: String,
                                          receiveUpdatesTimeout: Option[Int])

@Singleton
class TelegramApiClientConfigurationProvider @Inject()(config: Config) extends Provider[TelegramApiClientConfiguration] {
  lazy val get = TelegramApiClientConfiguration.fromConfig(config.getConfig("telegram"))
}