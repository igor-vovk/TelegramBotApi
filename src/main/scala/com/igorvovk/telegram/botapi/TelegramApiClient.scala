package com.igorvovk.telegram.botapi

import javax.inject.Inject

import akka.Done
import akka.stream.scaladsl.Source
import com.igorvovk.telegram.botapi.util.Funcs._
import play.api.Logger
import play.api.libs.json.{JsNull, JsObject, JsValue, Json}
import play.api.libs.ws.{StreamedResponse, WSClient}

import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions


case class TelegramApiClientException(message: String) extends RuntimeException(message)

object TelegramApiClient {

  def error(msg: String): Nothing = throw new TelegramApiClientException(msg)

  type ParseMode = Symbol

  object ParseMode {
    val Html = 'HTML
    val Markdown = 'Markdown
  }

  type ChatAction = Symbol

  object ChatAction {
    val Typing = 'typing
    val UploadPhoto = 'upload_photo
    val RecordVideo = 'record_video
    val UploadVideo = 'upload_video
    val RecordAudio = 'record_audio
    val UploadAudio = 'upload_audio
    val UploadDocument = 'upload_document
    val FindLocation = 'find_location
  }

  def filterNulls(obj: JsObject): JsObject = JsObject(obj.fields.filterNot(_._2 == JsNull))

}

class TelegramApiClient @Inject()(ws: WSClient,
                                  conf: TelegramApiClientConfiguration) {

  import TelegramApiClient._

  val log = Logger("application")

  def getUpdates(offset: Option[Long] = None, limit: Option[Int] = None)
                (implicit ec: ExecutionContext): Source[Update, _] = {
    val qp = Seq.empty ++
      mapO2P("offset", offset.map(_.toString)) ++
      mapO2P("limit", limit.map(_.toString)) ++
      mapO2P("timeout", conf.receiveUpdatesTimeout.map(_.toString))

    val f = ws.url(conf.apiBaseUrl + "/getUpdates")
      .withMethod("GET")
      .withQueryString(qp: _*)
      .stream()

    Source.fromFuture(f).flatMapConcat(_.body).flatMapConcat { bs =>
      val str = bs.utf8String
      log.debug("Body part: " + str)

      Source(Json.parse(str).as(responseReads[List[Update]]))
    }
  }

  def getMe()(implicit ec: ExecutionContext): Future[User] = {
    ws.url(conf.apiBaseUrl + "/getMe").get().map(_.json.as(responseReads[User]))
  }

  // https://core.telegram.org/bots/api#sendmessage
  def sendMessage(chatId: String, text: String, parseMode: Option[ParseMode] = None,
                  disableWebPagePreview: Option[Boolean] = None, disableNotification: Option[Boolean] = None,
                  replyToMessageId: Option[Long] = None, replyMarkup: Option[ReplyMarkup] = None)
                 (implicit ec: ExecutionContext): Future[Message] = {
    val params = filterNulls(Json.obj(
      "chat_id" -> chatId,
      "text" -> text,
      "parse_mode" -> parseMode.map(_.name),
      "disable_web_page_preview" -> disableWebPagePreview,
      "disable_notification" -> disableNotification,
      "reply_to_message_id" -> replyToMessageId,
      "reply_markup" -> replyMarkup
    ))

    ws.url(conf.apiBaseUrl + "/sendMessage").post(params).map(_.json.as(responseReads[Message]))
  }

  // https://core.telegram.org/bots/api#forwardmessage
  def forwardMessage(chatId: String, fromChatId: String, messageId: Long,
                     disableNotification: Option[Boolean] = None)
                    (implicit ec: ExecutionContext): Future[Message] = {
    val params = filterNulls(Json.obj(
      "chat_id" -> chatId,
      "from_chat_id" -> fromChatId,
      "disable_notification" -> disableNotification,
      "message_id" -> messageId
    ))

    ws.url(conf.apiBaseUrl + "/forwardMessage").post(params).map(_.json.as(responseReads[Message]))
  }

  // https://core.telegram.org/bots/api#getfile
  def getFile(fileId: String)(implicit ec: ExecutionContext): Future[File] = {
    ws.url(conf.apiBaseUrl + "/getFile").withQueryString("file_id" -> fileId).execute("GET").map(_.json.as(responseReads[File]))
  }

  def downloadFile(file: File): Future[StreamedResponse] = {
    ws.url(conf.downloadBaseUrl + "/" + file.file_path.get).withMethod("GET").stream()
  }

  // https://core.telegram.org/bots/api#sendchataction
  def sendChatAction(chatId: String, action: ChatAction)(implicit ec: ExecutionContext): Future[Done] = {
    val params = Json.obj(
      "chat_id" -> chatId,
      "action" -> action.name
    )

    ws.url(conf.apiBaseUrl + "/sendChatAction").post(params).map(_.json.as(responseReads[JsValue])).map(_ => Done)
  }

}
