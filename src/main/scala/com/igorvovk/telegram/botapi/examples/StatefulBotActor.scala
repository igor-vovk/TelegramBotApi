package com.igorvovk.telegram.botapi.examples

import javax.inject.{Provider, Singleton}

import akka.actor.{ActorLogging, Cancellable, PoisonPill, Props}
import akka.persistence._
import com.igorvovk.telegram.botapi.{Message, SendMessage}

@Singleton
class StatefulActorProvider extends Provider[Props] {
  lazy val get = Props(classOf[StatefulBotActor])
}


object StatefulBotActor {

  case class State(receivedMessages: Int = 0)

}

class StatefulBotActor extends PersistentActor with ActorLogging {

  import StatefulBotActor._
  import context._

  val persistenceId: String = "stateful-bot-" + parent.path.name

  var state: State = State()

  def receiveRecover = {
    case SnapshotOffer(_, snapshot: State) =>
      state = snapshot.copy(receivedMessages = snapshot.receivedMessages + state.receivedMessages)
  }

  def receiveCommand = {
    case m: Message =>
      state = state.copy(receivedMessages = state.receivedMessages + 1)

      sender() ! SendMessage("Received messages: " + state.receivedMessages)
  }

  override def postStop(): Unit = {
    log.info("Actor {} shutdown", self.path)

    if (state != null) {
      saveSnapshot(state)
    }
  }
}
