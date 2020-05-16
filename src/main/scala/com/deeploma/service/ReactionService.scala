package com.deeploma.service

import java.text.SimpleDateFormat
import java.util.{Date, UUID}

import com.deeploma.core
import com.deeploma.core._
import com.deeploma.domain.{Reminder, StockInterest, User}
import com.deeploma.repository.{InMemoryReminderRepository, InMemoryUserRepository}
import com.deeploma.utils._
import yahoofinance.YahooFinance

import scala.collection.JavaConverters._
import scala.util.Try

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
    val chatId: Long = event.chatId
    val maybeUser: Option[User] = getUserByTelegramChatId(chatId)
    val textMessage: String = event.text
    val userActions: Seq[Action] =
      if (maybeUser.isEmpty)
        Seq(
          TelegramAction(to = chatId, text = askForName),
          SaveOrUpdateUserAction(User(id = UUID.randomUUID(), telegramContext = Some(TelegramContext(chatId = chatId, lastActionDone = Some(TelegramAction(to = chatId, text = askForName))))))
        )
      else {
        val user: User = maybeUser.get
        val resultActions: Seq[Action] = parseRemindRequest(event) ++ parseStockRequest(event) ++ {
          user.telegramContext match {
            case Some(context) => context.lastActionDone match {
              case Some(TelegramAction(id, text)) if text == askForName =>
                val name = textMessage
                val niceToMeetYou = TelegramAction(id, s"Hi, $name! Nice to meet you!")
                Seq(
                  SaveOrUpdateUserAction(User(id = user.id, telegramContext = Some(TelegramContext(id, Some(niceToMeetYou))), userContext = Some(UserContext(name, Seq.empty)))),
                  niceToMeetYou
                )
              case Some(TelegramAction(id, _)) if textMessage.toLowerCase == "forget about it" => Seq(
                TelegramAction(id, "Ok, done")
              )
              case Some(TelegramAction(id, text)) if text.contains("you're asking to remind you at") =>
                val reminder = InMemoryReminderRepository.repo.findReminderByUserId(user.id).get
                if (textMessage.toLowerCase.contains("yes") || textMessage.toLowerCase.contains("right")) {
                  Seq(
                    SaveOrUpdateReminderAction(reminder.copy(wasConfirmed = true)),
                    LogReminderConfirmationAction(text, reminder.text, reminder.text),
                    TelegramAction(id, s"Ok, ${user.name}, I'll create a reminder for you")
                  )
                } else
                  Seq(
                    TelegramAction(id, s"Ok, ${user.name}, then what do you want me to remind you about at ${new SimpleDateFormat(dateFormat).format(reminder.time)}?"),
                  )
              case Some(TelegramAction(id, text)) if text.contains("then what do you want me to remind you about") =>
                val reminder = InMemoryReminderRepository.repo.findReminderByUserId(user.id).get
                Seq(
                  TelegramAction(id, "Ok, I'll create a reminder for you!"),
                  LogReminderConfirmationAction(reminder.text, reminder.text, textMessage),
                  SaveOrUpdateReminderAction(reminder.copy(wasConfirmed = true, text = textMessage))
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

  private def parseStockRequest(event: TelegramEvent): Seq[Action] = {
    val text: String = event.text.toLowerCase
    if (text.contains("stock") && !text.startsWith("stock")) {
      val chatId: Long = event.chatId
      val user: User = getUserByTelegramChatIdUnsafe(chatId)
      val words = text.split(" ")
      val indexOfStock = words.indexOf("stock")
      val stockName = words(indexOfStock - 1)
      val maybeStock = Try{
        YahooFinance.get(stockName.toUpperCase)
      }.toOption.nullToNone
      if(maybeStock.nonEmpty) {
        val stock = maybeStock.get
        val price = stock.getQuote.getPrice
        Seq(
          TelegramAction(chatId, s"$stockName is currently selling for ${price.toString}."),
          SaveOrUpdateUserAction(user.withNewInterest(StockInterest(stockName)))
        )
      }
      else {
        Seq(
          TelegramAction(chatId, "Sorry, I couldn't find this stock you are talking about. Maybe it's not in Yahoo's base yet or you need to check your spelling?")
        )
      }
    }
    else Seq.empty
  }

  private def parseRemindRequest(event: TelegramEvent): Seq[Action] = {
    val text: String = event.text.toLowerCase
    if (text.contains("remind")) {
      val chatId: Long = event.chatId
      val user: User = getUserByTelegramChatIdUnsafe(chatId)
      val whenDate: Date = new Date(parseTimeForReminder(text) + System.currentTimeMillis())
      val when: String = new SimpleDateFormat(dateFormat).format(whenDate)
      val what: String = text
      val confirmReminder: TelegramAction = TelegramAction(to = chatId, text = s"${user.name}, you're asking to remind you at $when to $what, right?")
      Seq(
        confirmReminder,
        SaveOrUpdateUserAction(user.withLastTelegramActionDone(confirmReminder)),
        SaveOrUpdateReminderAction(Reminder(UUID.randomUUID(), user.id, text, whenDate, wasSent = false, wasConfirmed = false))
      )
    } else
      Seq.empty
  }

  private def sorryTelegramEvent(chatId: Long): Action = {
    val userName = getUserByTelegramChatId(chatId).map(_.name).getOrElse("")
    TelegramAction(
      chatId,
      sorryNoAnswer.replace("{name}", userName)
    )
  }

  private def reminderEvent(event: ReminderEvent): Seq[Action] = {
    val user = getUserByUserIdUnsafe(event.reminder.userId)
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

  private def getUserByTelegramChatId(chatId: Long): Option[User] = InMemoryUserRepository.repository.getUserByTelegramChatId(chatId)

  private def getUserByTelegramChatIdUnsafe(chatId: Long): User = getUserByTelegramChatId(chatId).get

  private def getUserByUserId(userId: UUID): Option[User] = InMemoryUserRepository.repository.getUser(userId)

  private def getUserByUserIdUnsafe(userId: UUID): User = getUserByUserId(userId).get

  val askForName = "Hi, I don't know you, please, tell me your name"
  val sorryNoAnswer = "Sorry {name}, I don't know yet how to respond to your message. But I'm still learning"
  val dateFormat = "MM-dd-yyyy HH:mm:ss"
}
