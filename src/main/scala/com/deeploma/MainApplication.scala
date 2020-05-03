package com.deeploma

import com.deeploma.core._
import com.deeploma.domain.User
import com.deeploma.environments.{ClockEnvironment, TelegramEnvironment}
import com.deeploma.repository.InMemoryUserRepository
import com.deeploma.service.ReactionService

object MainApplication {

  def main(args: Array[String]): Unit = {
    work()
  }

  lazy val environments: Seq[Environment] = listAllEnvironments()

  def listAllEnvironments(): Seq[Environment] = Seq(
    ClockEnvironment.createEnvironment,
    TelegramEnvironment.env
  )

  def work(): Unit = {
    while (true) {
      val events: Seq[Event] = environments.flatMap(environment => environment.fetchEvents())
      val actions: Seq[Action] = reactToEvents(events)
      actions.foreach(doAction)
    }
  }

  def reactToEvents(events: Seq[Event]): Seq[Action] = {
    val reactions = for {
      event <- events
      reaction <- reactToEvent(event)
    } yield reaction
    reactions ++ ReactionService.checkForEmptyTelegramResponses(events, reactions)
  }

  private def reactToEvent(event: Event): Seq[Action] = ReactionService.reactToEvent(event)

  private def doAction(action: Action): Unit = action match {
    case LoggableAction(response) => println(response)
    case TelegramAction(to, text) => TelegramEnvironment.env.sendMessage(to, text)
    case SaveOrUpdateUserAction(id, telegramContext, userContext) => InMemoryUserRepository.repository.saveUser(User(
      id,
      telegramContext,
      userContext
    ))
  }

}
