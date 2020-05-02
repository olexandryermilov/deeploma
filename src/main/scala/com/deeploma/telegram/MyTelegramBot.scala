package com.deeploma.telegram

import com.bot4s.telegram.api.RequestHandler
import com.bot4s.telegram.clients.FutureSttpClient
import com.bot4s.telegram.future.{Polling, TelegramBot}
import com.bot4s.telegram.methods.SendMessage
import com.bot4s.telegram.models.Message
import com.softwaremill.sttp.SttpBackend
import com.softwaremill.sttp.okhttp.OkHttpFutureBackend

import scala.concurrent.Future

class MyTelegramBot(token: String) extends TelegramBot with Polling {

  implicit val backend: SttpBackend[Future, Nothing] = OkHttpFutureBackend()
  override val client: RequestHandler[Future] = new FutureSttpClient(token)
  var allMessages: Seq[Message] = Seq.empty

  override def receiveMessage(msg: Message): Future[Unit] = {
    allMessages = allMessages ++ Seq(msg)
    Future.successful()
  }

  def sendMessage(chatId: String, text: String): Future[Message] = request(SendMessage(chatId, text))
}

