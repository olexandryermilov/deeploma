package com.deeploma

import com.bot4s.telegram.models.{Chat, ChatType, Message}
import com.deeploma.core.{Action, Event, TelegramAction, TelegramEvent}
import com.deeploma.service.ReactionService
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

class E2ELogicTest extends Specification {
    "main flow" should {
      "work for happy path" in new baseCtx {
        var givenEvents: Seq[Event] = Seq(TelegramEvent(message))

        var expected: Seq[Action] = Seq(
          TelegramAction(to = chatId, text = ReactionService.askForName)
        )
        var actions: Seq[Action] = MainApplication.reactToEvents(givenEvents)
         actions must containAllOf(expected)
        actions.foreach(MainApplication.doAction)

        val name = "Sasha"
        givenEvents = Seq(TelegramEvent(message = message.copy(text= Some(name))))
        expected = Seq(
          TelegramAction(to = chatId, text = s"Hi, $name! Nice to meet you!")
        )

        actions = MainApplication.reactToEvents(givenEvents)
        actions must containAllOf(expected)
        actions.foreach(MainApplication.doAction)
        givenEvents = Seq(TelegramEvent(message = message.copy(text= Some(name))))
        expected = Seq(
          TelegramAction(to = chatId, text = ReactionService.sorryNoAnswer.replace("{name}", name))
        )

        actions = MainApplication.reactToEvents(givenEvents)
        actions must containAllOf(expected)
        actions.foreach(MainApplication.doAction)
      }
    }

  trait baseCtx extends Scope {
    val chatId = 13123123L
    val message: Message = Message(1000, date = 100000, chat = Chat(chatId, ChatType.Private))
  }
}
