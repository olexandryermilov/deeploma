package com.deeploma.service

import java.text.SimpleDateFormat

import com.deeploma.core.{Action, ClockEvent, Event, LoggableAction, TelegramAction, TelegramEvent}

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
      ).map(event => sorryTelegramEvent(event.asInstanceOf[TelegramEvent].message.chat.id))

  private def clockEvent(clockEvent: ClockEvent): Seq[Action] = Seq(
    //LoggableAction(response = new SimpleDateFormat("dd/MM/yyyy hh/mm/ss").format(clockEvent.time))
  )

  private def telegramEvent(event: TelegramEvent): Seq[Action] = Seq(
    LoggableAction(response = event.message.toString),
    //TelegramAction(to = event.message.chat.id, text = event.message.text.getOrElse(""))
  )

  private def unknownEvent(event: Event): Seq[Action] = Seq.empty

  private def sorryTelegramEvent(chatId: Long): Action = TelegramAction(
    chatId,
    "Sorry, I don't know yet how to respond to your message. But I'm still learning"
  )

}
