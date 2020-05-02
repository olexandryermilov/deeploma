package com.deeploma.service

import java.text.SimpleDateFormat

import com.deeploma.core.{Action, ClockEvent, Event, LoggableAction, TelegramAction, TelegramEvent}

object ReactionService {

  def reactToEvent(event: Event): Seq[Action] = event match {
    case event@ClockEvent(_) => clockEvent(event)
    case event@TelegramEvent(_) => telegramEvent(event)
    case event@_ => unknownEvent(event)
  }

  private def clockEvent(clockEvent: ClockEvent): Seq[Action] = Seq(
    LoggableAction(response = new SimpleDateFormat("dd/MM/yyyy hh/mm/ss").format(clockEvent.time))
  )
  private def telegramEvent(event: TelegramEvent): Seq[Action] = Seq(
    LoggableAction(response = event.message.toString),
    TelegramAction(to = event.message.chat.id, text = event.message.text.getOrElse(""))
  )

  private def unknownEvent(event: Event): Seq[Action] = Seq.empty

}
