package com.deeploma

import com.bot4s.telegram.models.{Chat, ChatType, Message}
import com.deeploma.core.{Action, Event, TelegramAction, TelegramEvent}
import com.deeploma.service.ReactionService
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

class E2ELogicTest extends Specification {
    "main flow" should {
      "work for happy path" in new baseCtx {
        val givenEvents: Seq[Event] = Seq(TelegramEvent(message))

        val expected: Seq[Action] = Seq(
          TelegramAction(to = chatId, text = ReactionService.askForName)
        )
        MainApplication.reactToEvents(givenEvents) must containAllOf(expected)
      }
    }

  trait baseCtx extends Scope {
    val chatId = 13123123L
    val message: Message = Message(1000, date = 100000, chat = Chat(chatId, ChatType.Private))
  }
}
