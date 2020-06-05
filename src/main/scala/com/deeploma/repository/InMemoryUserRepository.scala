package com.deeploma.repository

import java.util.UUID

import com.deeploma.core.{TelegramContext, UserContext}
import com.deeploma.domain.User

import scala.util.Try

class InMemoryUserRepository extends UserRepository {

  var usersStorage: Seq[User] = Seq.empty

  override def getUser(uuid: UUID): Option[User] = usersStorage.find(_.id == uuid)

  override def saveUser(user: User): Unit =
    usersStorage = usersStorage.filter(_.id != user.id) ++ Seq(user)

  override def getAllUsers(): Seq[User] = usersStorage

  override def getUserByTelegramChatId(chatId: Long): Option[User] =
    usersStorage.find(_.telegramContext.exists(_.chatId == chatId))

  override def updateUsersTelegramContext(id: UUID, newContext: Option[TelegramContext]): User = {
    val user = usersStorage.find(_.id == id).map(user => user.copy(telegramContext = Some(newContext.get.copy(allMessages = user.telegramContext.get.allMessages)))).get
    usersStorage = usersStorage.filter(_.id != id) ++ Seq(user)
    user
  }

  override def updateUserContext(id: UUID, newContext: Option[UserContext]): User = {
    val user = usersStorage.find(_.id == id).map(user => user.copy(userContext = newContext)).get
    usersStorage = usersStorage.filter(_.id != id) ++ Seq(user)
    user
  }

  override def updateUserMessageStory(chatId: Long, newMessage: String): User = {
    val user = usersStorage.find(_.telegramContext.get.chatId == chatId)
    val modified = user.map(user => user.copy(telegramContext = Some(user.telegramContext.get.copy(allMessages = user.telegramContext.get.allMessages ++ Seq(newMessage))))).get
    usersStorage = usersStorage.filter(_.telegramContext.get.chatId != chatId) ++ Seq(modified)
    modified
  }

  override def updateUserSentimentScore(chatId: Long, newScore: Double): User = {
    val user = usersStorage.find(_.telegramContext.get.chatId == chatId).getOrElse(User(UUID.randomUUID(), telegramContext = Some(TelegramContext(chatId, None, Seq.empty))))
    val modified = Try {
      user.copy(
        userContext = Some(user.userContext.get.copy(textsSentimentScore = user.userContext.get.textsSentimentScore ++ Seq(newScore)))
      )
    }.getOrElse(user)
    usersStorage = usersStorage.filter(_.telegramContext.get.chatId != chatId) ++ Seq(modified)
    modified
  }
}

object InMemoryUserRepository {
  val repository: UserRepository = createRepository

  private def createRepository: UserRepository = new InMemoryUserRepository
}
