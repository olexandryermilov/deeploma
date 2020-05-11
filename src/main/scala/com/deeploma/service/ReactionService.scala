package com.deeploma.service

import java.text.{DateFormat, SimpleDateFormat}
import java.util.{Date, UUID}

import com.deeploma.core._
import com.deeploma.domain.{Reminder, User}
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
    val textMessage = event.message.text.getOrElse("").toLowerCase
    val userActions: Seq[Action] =
      if (maybeUser.isEmpty)
        Seq(
          TelegramAction(to = chatId, text = askForName),
          SaveOrUpdateUserAction(User(id = UUID.randomUUID(), telegramContext = Some(TelegramContext(chatId = chatId, lastActionDone = Some(TelegramAction(to = chatId, text = askForName))))))
        )
      else {
        val user = maybeUser.get
        val resultActions = parseRemindRequest(event) ++ {
          user.telegramContext match {
            case Some(context) => context.lastActionDone match {
              case Some(TelegramAction(id, text)) if text == askForName =>
                val name = textMessage
                val niceToMeetYou = TelegramAction(id, s"Hi, $name! Nice to meet you!")
                Seq(
                  SaveOrUpdateUserAction(User(id = user.id, telegramContext = Some(TelegramContext(id, Some(niceToMeetYou))), userContext = Some(UserContext(name)))),
                  niceToMeetYou
                )
              case Some(TelegramAction(id, text)) if text.contains("you're asking to remind you at") =>
                val parsedDate = new SimpleDateFormat(dateFormat).parse(text.slice(text.indexOf("`") + 1, text.indexOf("~")))
                println(parsedDate)
                val remindText = text.slice(text.indexOf("&") + 1, text.indexOf("*"))
                if (textMessage.contains("yes") || textMessage.contains("right")) {
                  Seq(
                    SaveOrUpdateReminderAction(Reminder(UUID.randomUUID(), user.id, remindText, parsedDate, wasSent = false)),
                    TelegramAction(id, s"Ok, ${user.userContext.map(_.name).getOrElse("")}, I'll create a reminder for you")
                  )
                } else
                  Seq(
                    TelegramAction(id, s"Ok, ${user.userContext.map(_.name).getOrElse("")}, then what do you want me to remind you about at ${new SimpleDateFormat(dateFormat).format(parsedDate)}?"),
                  )
              case Some(TelegramAction(id, text)) if text.contains("then what do you want me to remind you about") =>
                val date = new SimpleDateFormat(dateFormat).parse(text.split(" ").takeRight(2).mkString(" ").dropRight(1))
                Seq(
                  TelegramAction(id, "Ok, I'll create a reminder for you!"),
                  SaveOrUpdateReminderAction(Reminder(UUID.randomUUID(), user.id, textMessage, date, wasSent = false))
                )
              case Some(TelegramAction(id, text)) if text.toLowerCase == "forget about it" => Seq(
                TelegramAction(id, "Ok, done")
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
    val text = event.message.text.getOrElse("").toLowerCase
    if (text.contains("remind")) {
      val chatId = event.message.chat.id
      val user = InMemoryUserRepository.repository.getUserByTelegramChatId(chatId).get
      val when: String = new SimpleDateFormat(dateFormat).format(new Date(parseTimeForReminder(text) + System.currentTimeMillis()))
      val what = text
      val confirmReminder = TelegramAction(to = chatId, text = s"${user.userContext.get.name}, you're asking to remind you at `$when~ to &$what*, right?")
      Seq(
        //TelegramAction(to = chatId, text = s"Ok, ${user.userContext.get.name}! I'll create a reminder for you"),
        confirmReminder,
        SaveOrUpdateUserAction(user.withLastTelegramActionDone(confirmReminder)),
        //SaveOrUpdateReminderAction(Reminder(UUID.randomUUID(), user.id, text, when, wasSent = false))
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
        event.reminder.copy(
          wasSent = true
        ))
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
  val dateFormat = "MM-dd-yyyy HH:mm:ss"
}
