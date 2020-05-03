package com.deeploma.service

import java.text.SimpleDateFormat
import java.util.UUID

import com.deeploma.core.{Action, ClockEvent, Event, LoggableAction, SaveOrUpdateUserAction, TelegramAction, TelegramContext, TelegramEvent}
import com.deeploma.repository.InMemoryUserRepository

object ReactionService {

  def reactToEvent(event: Event): Seq[Action] = event match {
    case event@ClockEvent(_) => clockEvent(event)
    case event@TelegramEvent(_) => telegramEvent(event)
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
        println(user)
        user.telegramContext match {
          case Some(context) => context.lastActionDone match {
            case Some(TelegramAction(id, text)) if text == askForName =>
              val name = event.message.text.get
              val niceToMeetYou = TelegramAction(id, s"Hi, $name! Nice to meet you!")
              Seq(
                SaveOrUpdateUserAction(id = user.id, telegramContext = Some(TelegramContext(id, Some(niceToMeetYou)))),
                niceToMeetYou
              )
            case _ => Seq.empty
          }
          case None => Seq.empty
        }
      }
    Seq(
      LoggableAction(response = event.message.toString),
    ) ++ userActions
  }

  private def unknownEvent(event: Event): Seq[Action] = Seq.empty

  private def sorryTelegramEvent(chatId: Long): Action = TelegramAction(
    chatId,
    sorryNoAnswer
  )

  private val askForName = "Hi, I don't know you, please, tell me your name"
  private val sorryNoAnswer = "Sorry, I don't know yet how to respond to your message. But I'm still learning"
}
