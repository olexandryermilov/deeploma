package com.deeploma.domain

import java.util.UUID

import com.deeploma.core.{TelegramContext, UserContext}

case class User(id: UUID,
                telegramContext: Option[TelegramContext] = None,
                userContext: Option[UserContext] = None
               )
