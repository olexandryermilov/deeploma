package com.deeploma.service

import java.util.{Date, UUID}

import com.deeploma.core._
import com.deeploma.repository.InMemoryUserRepository

object ReactionService {

  def reactToEvent(event: Event): Seq[Action] = event match {
    case event@ClockEvent(_) => clockEvent(event)
    case event@TelegramEvent(_) => telegramEvent(event)
    case event@ReminderEvent(_) => reminderEvent(event)
    case event@_ => unknownEvent(event)
  }

  def checkForEmptyTelegramResponses(events: Seq[Event], reactions: Seq[Action]): Seq[Action] =
    events
      .filter(event => event.isInstanceOf[TelegramEvent])
      .filterNot(event => reactions
        .filter(reaction => reaction.isInstanceOf[TelegramAction])
        .exists(reaction => reaction.asInstanceOf[TelegramAction].to == event.asInstanceOf[TelegramEvent].message.chat.id)
      ).map(event => {
      NoResponseLogger.handleNoResponseEvent(event)
      sorryTelegramEvent(event.asInstanceOf[TelegramEvent].message.chat.id)
    })

  private def clockEvent(clockEvent: ClockEvent): Seq[Action] = Seq(
    //LoggableAction(response = new SimpleDateFormat("dd/MM/yyyy hh/mm/ss").format(clockEvent.time))
  )

  private def telegramEvent(event: TelegramEvent): Seq[Action] = {
    val chatId = event.message.chat.id
    val maybeUser = InMemoryUserRepository.repository.getUserByTelegramChatId(chatId)
    val userActions: Seq[Action] =
      if (maybeUser.isEmpty)
        Seq(
          TelegramAction(to = chatId, text = askForName),
          SaveOrUpdateUserAction(id = UUID.randomUUID(), telegramContext = Some(TelegramContext(chatId = chatId, lastActionDone = Some(TelegramAction(to = chatId, text = askForName)))))
        )
      else {
        val user = maybeUser.get
        val resultActions = parseRemindRequest(event) ++ {
          user.telegramContext match {
            case Some(context) => context.lastActionDone match {
              case Some(TelegramAction(id, text)) if text == askForName =>
                val name = event.message.text.get
                val niceToMeetYou = TelegramAction(id, s"Hi, $name! Nice to meet you!")
                Seq(
                  SaveOrUpdateUserAction(id = user.id, telegramContext = Some(TelegramContext(id, Some(niceToMeetYou))), userContext = Some(UserContext(name))),
                  niceToMeetYou
                )
              case _ => Seq.empty
            }
            case None => Seq.empty
          }
        }
        resultActions
      }
    Seq(
      //LoggableAction(response = event.message.toString),
    ) ++ userActions
  }

  private def unknownEvent(event: Event): Seq[Action] = Seq.empty

  private def parseRemindRequest(event: TelegramEvent): Seq[Action] = {
    val text = event.message.text.getOrElse("")
    if (text.contains("remind")) {
      val chatId = event.message.chat.id
      val user = InMemoryUserRepository.repository.getUserByTelegramChatId(chatId).get
      val when: Date = new Date(parseTimeForReminder(text) + System.currentTimeMillis())
      Seq(
        TelegramAction(to = chatId, text = s"Ok, ${user.userContext.get.name}! I'll create a reminder for you"),
        SaveOrUpdateReminderAction(UUID.randomUUID(), user.id, text, when, wasSent = false)
      )
    } else
      Seq.empty
  }

  private def sorryTelegramEvent(chatId: Long): Action = {
    val userName = InMemoryUserRepository.repository.getUserByTelegramChatId(chatId).flatMap(_.userContext.map(_.name)).getOrElse("")
    TelegramAction(
      chatId,
      sorryNoAnswer.replace("{name}", userName)
    )
  }

  private def reminderEvent(event: ReminderEvent): Seq[Action] = {
    val user = InMemoryUserRepository.repository.getUser(event.reminder.userId).get
    Seq(
      user.telegramContext.map(context => TelegramAction(
        to = context.chatId,
        text = event.reminder.text
      )).getOrElse(EmptyAction()),
      SaveOrUpdateReminderAction(
        event.reminder.id,
        event.reminder.userId,
        event.reminder.text,
        event.reminder.time,
        wasSent = true
      )
    )
  }

  private def parseTimeForReminder(text: String): Long = {
    val timeMeasures = Seq("seconds", "minutes", "hours", "second", "minute", "hour")
    val words = text.split(" ")
    timeMeasures.map(measurement => {
      val measurementIndex = words.indexOf(measurement)
      if (measurementIndex > 0) words(measurementIndex - 1).toInt * measurementToMilliSeconds(measurement)
      else 0
    }).sum
  }

  private def measurementToMilliSeconds(measurement: String): Int =
    if (measurement.contains("second")) 1000
    else if (measurement.contains("minute")) 1000 * 60
    else if (measurement.contains("hour")) 1000 * 60 * 60
    else 0

  val askForName = "Hi, I don't know you, please, tell me your name"
  val sorryNoAnswer = "Sorry {name}, I don't know yet how to respond to your message. But I'm still learning"
}
