package com.deeploma.core

import java.util.{Date, UUID}

import com.bot4s.telegram.models.Message
import com.deeploma.domain.Reminder

sealed trait Event

/**
 * abstract events
 */
case class UserEvent(userId: UUID) extends Event

/**
 * specific events
 */
case class ClockEvent(time: Date) extends Event
case class TelegramEvent(message: Message) extends Event
case class ReminderEvent(reminder: Reminder) extends Event
