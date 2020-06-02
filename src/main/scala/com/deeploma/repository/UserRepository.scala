package com.deeploma.repository

import java.util.UUID

import com.deeploma.core.{TelegramContext, UserContext}
import com.deeploma.domain.User

trait UserRepository {

  def getUser(uuid: UUID): Option[User]

  def getUserByTelegramChatId(chatId: Long): Option[User]

  def saveUser(user: User): Unit

  def getAllUsers(): Seq[User]

  def updateUsersTelegramContext(uuid: UUID, newContext: Option[TelegramContext]): User

  def updateUserContext(uuid: UUID, newContext: Option[UserContext]): User

  def updateUserMessageStory(chatId: Long, newMessage: String): User
}
