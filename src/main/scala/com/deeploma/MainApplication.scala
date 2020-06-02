package com.deeploma

import java.io.FileWriter

import com.deeploma.core._
import com.deeploma.environments.{ReminderEnvironment, TelegramEnvironment}
import com.deeploma.repository.{InMemoryReminderRepository, InMemoryUserRepository}
import com.deeploma.service.ReactionService
import com.deeploma.service.ReactionService.mapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule

object MainApplication {

  def main(args: Array[String]): Unit = {
    mapper.registerModule(DefaultScalaModule)
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
      if (events.nonEmpty) println(events)
      val actions: Seq[Action] = reactToEvents(events)
      actions.foreach(doAction)
    }
  }

  def reactToEvents(events: Seq[Event]): Seq[Action] = {
    val reactions = for {
      event <- events
      reaction <- reactToEvent(event)
    } yield reaction
    if (reactions.nonEmpty) println(reactions)
    reactions ++ ReactionService.checkForEmptyTelegramResponses(events, reactions)
  }

  private def reactToEvent(event: Event): Seq[Action] = ReactionService.reactToEvent(event)

  def doAction(action: Action): Unit = action match {
    case a@LoggableAction(response) => println(response)
      println(a)
    case action@TelegramAction(to, text) => {
      println(action)
      TelegramEnvironment.env.sendMessage(to, text)
      val user = InMemoryUserRepository.repository.getUserByTelegramChatId(chatId = to)
      if (user.nonEmpty) doAction(SaveOrUpdateUserAction(user.get.withLastTelegramActionDone(action)))
    }
    case action@SaveOrUpdateUserAction(user) =>
      println(action)
      InMemoryUserRepository.repository.saveUser(user)
    case action@SaveOrUpdateReminderAction(reminder) =>
      println(action)
      InMemoryReminderRepository.repo.saveOrUpdateReminder(reminder)
    case action@LogReminderConfirmationAction(text, parsed, confirmed) =>
      println(action)
      logReminder(text, parsed, confirmed)
    case action@LogMessageType(text, messageType) =>
      println(action)
      logMessage(text, messageType.toString)
    case action@UpdateMessageHistoryAction(chatId, newMessage) =>
      println(action)
      InMemoryUserRepository.repository.updateUserMessageStory(chatId, newMessage)
    case action@EmptyAction() =>
  }

  private def logReminder(text: String, parsed: String, confirmed: String): Unit = {
    val fw = new FileWriter("datasets/reminders.csv", true)
    try {
      fw.write(s"\n$text,$parsed,$confirmed")
    }
    finally fw.close()
  }

  private def logMessage(text: String, messageType: String): Unit = {
    val fw = new FileWriter("datasets/messages.csv", true)
    try {
      fw.write(
        s"""
           |"$text",$messageType""".stripMargin)
    }
    finally fw.close()
  }

}
