package com.igorvovk.telegram.botapi

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Status, Terminated}
import akka.stream.{ActorMaterializer, Materializer}
import akka.stream.scaladsl.Sink

import scala.concurrent.duration._
import scala.language.postfixOps

object UpdatesEmitterActor {

  case object StartReceivingUpdates

  case class AddSubscriber(actor: ActorRef)

  def apply(client: TelegramApiClient) = Props(classOf[UpdatesEmitterActor], client)

}

class UpdatesEmitterActor(client: TelegramApiClient) extends Actor with ActorLogging {

  implicit val mat: Materializer = ActorMaterializer()

  import UpdatesEmitterActor._
  import context._

  val sink = Sink.actorRef[Update](self, StartReceivingUpdates)

  var lastOffset: Option[Long] = None
  var subscribers: Set[ActorRef] = Set.empty
  var stopped: Boolean = false

  def receive = {
    case StartReceivingUpdates if !stopped =>
      client.getUpdates(lastOffset.map(_ + 1L)).runWith(sink)
    case Status.Failure(e) => // Failure from getUpdates stream
      val repeatDelay = 10 seconds

      log.error(e, "Error while calling getUpdates, repeating after {}", repeatDelay)
      system.scheduler.scheduleOnce(repeatDelay, self, StartReceivingUpdates)
    case u@Update(id, data) => // New message from getUpdates stream
      lastOffset = Some(id)

      subscribers.foreach(_ ! data)
    case AddSubscriber(actor) =>
      log.debug("New subscriber {}", actor.path)

      subscribers = subscribers + watch(actor)
    case Terminated(actor) =>
      subscribers = subscribers - actor
  }

  @throws(classOf[Exception])
  override def postStop(): Unit = {
    subscribers.foreach(unwatch)

    stopped = true
  }
}
