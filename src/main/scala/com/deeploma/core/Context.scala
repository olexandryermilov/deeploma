package com.deeploma.core

sealed trait Context

case class UserContext(name: String) extends Context

case class TelegramContext(chatId: Long, lastActionDone: Option[TelegramAction]) extends Context
