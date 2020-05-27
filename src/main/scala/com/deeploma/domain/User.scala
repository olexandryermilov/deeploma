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

  def withNewInterest(interest: Interest): User = this.copy(
    userContext = userContext.map(_.copy(interests = this.userContext.get.interests ++ Seq(interest)))
  )

  def withNewMessage(messageText: String): User = this.copy(
    telegramContext = telegramContext.map(_.copy(allMessages = this.telegramContext.get.allMessages ++ Seq(messageText)))
  )

  def name: String = userContext.map(_.name).getOrElse("")

  def interests: Seq[Interest] = userContext.map(_.interests).getOrElse(Seq.empty)
}
