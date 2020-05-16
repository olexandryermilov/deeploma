package com.deeploma.core

import com.deeploma.domain.Interest

sealed trait Context

case class UserContext(name: String, interests: Seq[Interest]) extends Context

case class TelegramContext(chatId: Long, lastActionDone: Option[TelegramAction]) extends Context
