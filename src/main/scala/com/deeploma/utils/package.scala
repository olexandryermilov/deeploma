package com.deeploma

import com.deeploma.core.TelegramEvent

package object utils {

  implicit class TelegramEventExtensions(val event: TelegramEvent) extends AnyVal {
    def chatId: Long = event.message.chat.id

    def text: String = event.message.text.getOrElse("")
  }

  implicit class AnyExtensions[T](val any: Option[T]) extends AnyVal {
    def nullToNone: Option[T] = if (any.nonEmpty && any.get == null) None else any
  }

}
