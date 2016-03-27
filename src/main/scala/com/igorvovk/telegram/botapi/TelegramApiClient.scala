package com.igorvovk.telegram.botapi

import javax.inject.Inject

import akka.Done
import akka.stream.scaladsl.Source
import com.igorvovk.telegram.botapi.util.Funcs._
import play.api.Logger
import play.api.libs.json._
import play.api.libs.ws.{StreamedResponse, WSClient}

import scala.concurrent.{ExecutionContext, Future}


case class TelegramApiClientException(message: String) extends RuntimeException(message)

object TelegramApiClient {

  def error(msg: String): Nothing = throw new TelegramApiClientException(msg)

  def responseReads[T](implicit rds: Reads[T]): Reads[T] = Reads(jv => {
    if ((jv \ "ok").validate[Boolean].getOrElse(false)) {
      (jv \ "result").validate[T]
    } else {
      (jv \ "description").validate[String].flatMap(JsError.apply)
    }
  })

}

class TelegramApiClient @Inject()(ws: WSClient,
                                  conf: TelegramApiClientConfiguration) {

  import TelegramApiClient._

  val log = Logger("application")

  lazy val botId: Long = conf.credentials.id

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

  // https://core.telegram.org/bots/api#getfile
  def getFile(fileId: String)(implicit ec: ExecutionContext): Future[File] = {
    ws.url(conf.apiBaseUrl + "/getFile").withQueryString("file_id" -> fileId).execute("GET").map(_.json.as(responseReads[File]))
  }

  def downloadFile(file: File): Future[StreamedResponse] = {
    ws.url(conf.downloadBaseUrl + "/" + file.file_path.get).withMethod("GET").stream()
  }

  def send(chatId: String, text: String)(implicit ec: ExecutionContext): Future[Message] = {
    send(chatId, SendMessage(text))
  }

  // https://core.telegram.org/bots/api#sendmessage
  def send(chatId: String, o: OutMessage)(implicit ec: ExecutionContext): Future[Message] = {
    o match {
      case s: SendMessage => doSend[SendMessage, Message](chatId, s, "/sendMessage")
      case f: ForwardMessage => doSend[ForwardMessage, Message](chatId, f, "/forwardMessage")
      case l: SendLocation => doSend[SendLocation, Message](chatId, l, "/sendLocation")
    }
  }

  // https://core.telegram.org/bots/api#sendchataction
  def sendChatAction(chatId: String, c: SendChatAction)(implicit ec: ExecutionContext): Future[Done] = {
    doSend[SendChatAction, JsValue](chatId, c, "/sendChatAction").map(_ => Done)
  }

  private def doSend[Req, Resp](chatId: String, req: Req, pathSuffix: String)
                             (implicit ec: ExecutionContext, wr: OWrites[Req], rds: Reads[Resp]): Future[Resp] = {
    val params = chatIdJson(chatId) ++ wr.writes(req)

    ws.url(conf.apiBaseUrl + pathSuffix).post(params).map(_.json.as(responseReads[Resp]))
  }

  private def chatIdJson(chid: String): JsObject = {
    Json.obj(
      "chat_id" -> chid
    )
  }

}
