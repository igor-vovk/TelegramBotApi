package com.igorvovk.telegram.botapi

import javax.inject.{Inject, Provider, Singleton}

import play.api.Configuration

object TelegramApiClientConfiguration {

  object Token {
    def fromConfig(config: Configuration): Option[Token] = {
      for {
        id <- config.getLong("id")
        secret <- config.getString("secret")
      } yield Token(id, secret)
    }
  }

  case class Token(id: Long, secret: String) {
    lazy val str = s"$id:$secret"
  }

  def fromConfig(config: Configuration): TelegramApiClientConfiguration = {
    val token = config.getConfig("bot.token").flatMap(Token.fromConfig).get

    TelegramApiClientConfiguration(
      credentials = token,
      apiBaseUrl = config.getString("apiUrl").get + "/bot" + token.str,
      downloadBaseUrl = config.getString("apiUrl").get + "/file/bot" + token.str,
      receiveUpdatesTimeout = Some(90)
    )
  }

}

case class TelegramApiClientConfiguration(credentials: TelegramApiClientConfiguration.Token,
                                          apiBaseUrl: String,
                                          downloadBaseUrl: String,
                                          receiveUpdatesTimeout: Option[Int])

@Singleton
class TelegramApiClientConfigurationProvider @Inject()(config: Configuration) extends Provider[TelegramApiClientConfiguration] {
  lazy val get = TelegramApiClientConfiguration.fromConfig(config.getConfig("telegram").get)
}