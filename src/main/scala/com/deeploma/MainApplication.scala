package com.deeploma

import com.deeploma.core._
import com.deeploma.domain.{Reminder, User}
import com.deeploma.environments.{ClockEnvironment, ReminderEnvironment, TelegramEnvironment}
import com.deeploma.repository.{InMemoryReminderRepository, InMemoryUserRepository}
import com.deeploma.service.ReactionService

object MainApplication {

  def main(args: Array[String]): Unit = {
    work()
  }

  lazy val environments: Seq[Environment] = listAllEnvironments()

  def listAllEnvironments(): Seq[Environment] = Seq(
    //ClockEnvironment.createEnvironment,
    TelegramEnvironment.env,
    ReminderEnvironment.createEnvironment()
  )

  def work(): Unit = {
    while (true) {
      val events: Seq[Event] = environments.flatMap(environment => environment.fetchEvents())
      if(events.nonEmpty)println(events)
      val actions: Seq[Action] = reactToEvents(events)
      actions.foreach(doAction)
    }
  }

  def reactToEvents(events: Seq[Event]): Seq[Action] = {
    val reactions = for {
      event <- events
      reaction <- reactToEvent(event)
    } yield reaction
    if(reactions.nonEmpty)println(reactions)
    reactions ++ ReactionService.checkForEmptyTelegramResponses(events, reactions)
  }

  private def reactToEvent(event: Event): Seq[Action] = ReactionService.reactToEvent(event)

  def doAction(action: Action): Unit = action match {
    case LoggableAction(response) => println(response)
    case TelegramAction(to, text) => TelegramEnvironment.env.sendMessage(to, text)
    case SaveOrUpdateUserAction(id, telegramContext, userContext) => InMemoryUserRepository.repository.saveUser(User(
      id, telegramContext, userContext
    ))
    case SaveOrUpdateReminderAction(id, userId, text, when, wasSent) => InMemoryReminderRepository.repo.saveOrUpdateReminder(Reminder(
      id, userId, text, when, wasSent
    ))
    case EmptyAction() =>
  }

}
