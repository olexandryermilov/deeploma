package com.deeploma.environments

import com.bot4s.telegram.models.Message
import com.deeploma.core.{Environment, Event, TelegramEvent}
import com.deeploma.telegram.MyTelegramBot

import scala.concurrent.Future
import scala.io.Source

class TelegramEnvironment(myTelegramBot: MyTelegramBot) extends Environment {
  override def fetchEvents(): Seq[Event] = {
    val messages = myTelegramBot.allMessages
    myTelegramBot.allMessages = Seq.empty
    messages.map(TelegramEvent)
  }

  def sendMessage(chatId: Long, text: String): Future[Message] = myTelegramBot.sendMessage(chatId.toString, text)
}

object TelegramEnvironment {

  lazy val env = createEnvironment

  private def createEnvironment: TelegramEnvironment = {
    val bot: MyTelegramBot = new MyTelegramBot(readToken)
    bot.run()
    new TelegramEnvironment(bot)
  }



  private def readToken: String = Source.fromResource("bot.token").getLines.mkString
}
