package com.igorvovk.telegram.botapi.examples

import akka.actor.{ActorLogging, Cancellable, Props}
import akka.persistence._
import com.igorvovk.telegram.botapi.{Chat, Message, TelegramApiClient}

import scala.concurrent.duration._
import scala.language.postfixOps

object StatefulBotActor {

  def props(client: TelegramApiClient): Props = Props(classOf[StatefulBotActor], client)

  case class State(chat: Chat, receivedMessages: Int = 0)

  case object TimeToSayGoodbye

}

class StatefulBotActor(client: TelegramApiClient) extends PersistentActor with ActorLogging {

  import StatefulBotActor._
  import context._

  val persistenceId: String = "stateful-bot-" + self.path.name

  var state: State = _
  var shutdownTimer: Option[Cancellable] = None

  def receiveRecover = {
    case SnapshotOffer(_, snapshot: State) => state = snapshot
  }

  def receiveCommand = {
    case m: Message =>
      if (null == state) {
        state = State(m.chat)
      }

      state = state.copy(receivedMessages = state.receivedMessages + 1)

      client.sendMessage(state.chat.id.toString, "Received messages: " + state.receivedMessages).onFailure {
        case e => log.error(e, "Error when sending message")
      }

      scheduleShutdown()
    case TimeToSayGoodbye =>
      log.info("Actor {} shutdown", self.path)

      if (state != null) {
        saveSnapshot(state)
      }

      stop(self)
    case SaveSnapshotSuccess(_) => // Do nothing
    case SaveSnapshotFailure(_, e) => log.error(e, "Can't save snapshot")
  }

  def scheduleShutdown(): Unit = {
    val delay = 30 seconds

    shutdownTimer.foreach(_.cancel())
    shutdownTimer = Some(system.scheduler.scheduleOnce(delay, self, TimeToSayGoodbye))
  }

  override def postStop(): Unit = {
    shutdownTimer.foreach(_.cancel())

    super.postStop()
  }
}
