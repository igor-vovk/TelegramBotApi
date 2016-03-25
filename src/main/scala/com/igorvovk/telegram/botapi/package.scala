package com.igorvovk.telegram

import play.api.libs.json._

package object botapi {

  implicit val symbolFormat: Format[Symbol] = Format(
    Reads(_.validate[String].map(Symbol.apply)),
    Writes(s => JsString(s.name))
  )

  def responseReads[T](implicit rds: Reads[T]): Reads[T] = Reads(jv => {
    if ((jv \ "ok").validate[Boolean].getOrElse(false)) {
      (jv \ "result").validate[T]
    } else {
      (jv \ "description").validate[String].flatMap(JsError.apply)
    }
  })


  // https://core.telegram.org/bots/api#user
  case class User(id: Long, first_name: String, last_name: Option[String], username: Option[String])
  implicit val userFmt = Json.format[User]

  // https://core.telegram.org/bots/api#location
  case class Location(longitude: Double, latitude: Double) extends MessageData
  implicit val locationFmt = Json.format[Location]

  type ChatType = Symbol
  object ChatType {
    val Private = 'private
    val Group = 'group
    val Semigroup = 'semigroup
    val Channel = 'channel
  }

  // https://core.telegram.org/bots/api#chat
  case class Chat(id: Long, `type`: ChatType, title: Option[String], username: Option[String],
                  first_name: Option[String], last_name: Option[String])
  implicit val chatFmt = Json.format[Chat]

  // https://core.telegram.org/bots/api#audio
  case class Audio(file_id: String, duration: Int, performer: Option[String], title: Option[String],
                   mime_type: Option[String], file_size: Option[Int]) extends MessageData
  implicit val audioFmt = Json.format[Audio]

  // https://core.telegram.org/bots/api#voice
  case class Voice(filed_id: String, duration: Int, mime_type: Option[String],
                   file_size: Option[Int]) extends MessageData
  implicit val voiceFmt = Json.format[Voice]

  // https://core.telegram.org/bots/api#contact
  case class Contact(phone_number: String, first_name: String, last_name: Option[String],
                     user_id: Option[Long]) extends MessageData
  implicit val contactFmt = Json.format[Contact]

  // https://core.telegram.org/bots/api#message
  case class MessageText(text: String) extends MessageData
  case class MessageForward(from: User, date: Long)
  implicit val messageForwardReads: Reads[Option[MessageForward]] = {
    for {
      userOpt <- (__ \ "forward_from").readNullable[User]
      dateOpt <- (__ \ "forward_date").readNullable[Long]
    } yield for {
      user <- userOpt
      date <- dateOpt
    } yield MessageForward(user, date)
  }

  sealed trait MessageData
  implicit val messageDataListReads: Reads[List[MessageData]] = {
    for {
      textOpt <- (__ \ "text").readNullable[String].map(_.map(MessageText.apply))
      captionOpt <- (__ \ "caption").readNullable[String].map(_.map(MessageText.apply))
      audioOpt <- (__ \ "audio").readNullable[Audio]
      voiceOpt <- (__ \ "voice").readNullable[Voice]
      contactOpt <- (__ \ "contact").readNullable[Contact]
      locationOpt <- (__ \ "location").readNullable[Location]
    } yield {
      val dataBldr = List.newBuilder[MessageData]
      textOpt.foreach(dataBldr.+=)
      captionOpt.foreach(dataBldr.+=)
      audioOpt.foreach(dataBldr.+=)
      voiceOpt.foreach(dataBldr.+=)
      contactOpt.foreach(dataBldr.+=)
      locationOpt.foreach(dataBldr.+=)

      dataBldr.result()
    }
  }


  sealed trait ServiceMessageData extends MessageData


  case class Message(message_id: Long, from: Option[User], date: Long, chat: Chat, forward: Option[MessageForward],
                     reply_to_message: Option[Message], data: List[MessageData]) extends UpdateData
  implicit val messageReads: Reads[Message] = {
    for {
      id <- (__ \ "message_id").read[Long]
      from <- (__ \ "from").readNullable[User]
      date <- (__ \ "date").read[Long]
      chat <- (__ \ "chat").read[Chat]
      forwardFrom <- __.read[Option[MessageForward]]
      replyToMessage <- (__ \ "reply_to_message").readNullable[Message]
      messageData <- __.read(messageDataListReads)
    } yield {
      Message(id, from, date, chat, forwardFrom, replyToMessage, messageData)
    }
  }

  // https://core.telegram.org/bots/api#inlinequery
  case class InlineQuery(id: String, from: User, query: String, offset: String) extends UpdateData
  implicit val inlineQueryFmt = Json.format[InlineQuery]

  // https://core.telegram.org/bots/api#choseninlineresult
  case class ChosenInlineResult(result_id: String, from: User, query: String) extends UpdateData
  implicit val chosenInlineResultFmt = Json.format[ChosenInlineResult]

  // https://core.telegram.org/bots/api#update
  sealed trait UpdateData
  case class Update(update_id: Long, data: UpdateData)

  implicit val updateReads: Reads[Update] = {
    /*val missingErr = JsError("Either message, inline_query or chosen_inline_result should be passed")*/

    for {
      id <- (__ \ "update_id").read[Long]
      messageOpt <- (__ \ "message").readNullable[Message]
      inlineQueryOpt <- (__ \ "inline_query").readNullable[InlineQuery]
      chosenInlineResultOpt <- (__ \ "chosen_inline_result").readNullable[ChosenInlineResult]
    } yield Update(id, messageOpt.orElse(inlineQueryOpt).orElse(chosenInlineResultOpt).get)
  }

  case class File(file_id: String, file_size: Option[Int], file_path: Option[String])
  implicit val fileReads = Json.reads[File]

  // https://core.telegram.org/bots/api#replykeyboardmarkup
  case class ReplyKeyboardMarkup(keyboard: Array[Array[String]], resize_keyboard: Boolean = false,
                                 one_time_keyboard: Boolean = false, selective: Boolean = false) extends ReplyMarkup
  implicit val replyKeyboardMarkupFmt = Json.format[ReplyKeyboardMarkup]

  case class ReplyKeyboardHide(hide_keyboard: Boolean = true, selective: Boolean = false) extends ReplyMarkup
  implicit val replyKeyboardHideFmt = Json.format[ReplyKeyboardHide]

  case class ForceReply(force_reply: Boolean = true, selective: Boolean = false) extends ReplyMarkup
  implicit val forceReplyFmt = Json.format[ForceReply]

  sealed trait ReplyMarkup
  implicit val replyMarkupWrites = OWrites[ReplyMarkup] {
    case a: ReplyKeyboardMarkup => replyKeyboardMarkupFmt.writes(a)
    case a: ReplyKeyboardHide => replyKeyboardHideFmt.writes(a)
    case a: ForceReply => forceReplyFmt.writes(a)
  }

}
