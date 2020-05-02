package com.deeploma.service

import com.deeploma.core.{Event, TelegramEvent}

object NoResponseLogger {

  def handleNoResponseEvent(event: Event): Unit = event match {
    case event@TelegramEvent(_) => logTelegramEvent(event)
  }

  private def logTelegramEvent(telegramEvent: TelegramEvent): Unit =
    println(telegramEvent)
}
