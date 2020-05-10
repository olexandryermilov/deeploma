package com.deeploma.domain

import java.util.UUID

import com.deeploma.core.{TelegramAction, TelegramContext, UserContext}

case class User(id: UUID,
                telegramContext: Option[TelegramContext] = None,
                userContext: Option[UserContext] = None
               ) {
  def withLastTelegramActionDone(action: TelegramAction): User = this.copy(
    telegramContext = telegramContext.map(_.copy(lastActionDone = Some(action)))
  )
}
