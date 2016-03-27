package com.igorvovk.telegram.botapi.examples

import akka.actor.{ActorLogging, Cancellable, PoisonPill, Props}
import akka.persistence._
import com.igorvovk.telegram.botapi.{Message, SendMessage}

object StatefulBotActor {

  def apply(): Props = Props(classOf[StatefulBotActor])

  case class State(receivedMessages: Int = 0)

}

class StatefulBotActor extends PersistentActor with ActorLogging {

  import StatefulBotActor._
  import context._

  val persistenceId: String = "stateful-bot-" + parent.path.name

  var state: State = State()
  var shutdownTimer: Option[Cancellable] = None

  def receiveRecover = {
    case SnapshotOffer(_, snapshot: State) =>
      state = snapshot.copy(receivedMessages = snapshot.receivedMessages + state.receivedMessages)
  }

  def receiveCommand = {
    case m: Message =>
      state = state.copy(receivedMessages = state.receivedMessages + 1)

      sender() ! SendMessage("Received messages: " + state.receivedMessages)
    case PoisonPill =>
      log.info("Actor {} shutdown", self.path)

      if (state != null) {
        saveSnapshot(state)
      }

      stop(self)
    case SaveSnapshotSuccess(_) => // Do nothing
    case SaveSnapshotFailure(_, e) => log.error(e, "Can't save snapshot")
  }
}
