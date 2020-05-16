package com.deeploma.core

import com.deeploma.domain.{MessageType, Reminder, User}

sealed trait Action

case class EmptyAction() extends Action

case class LoggableAction(response: String) extends Action

case class LogReminderConfirmationAction(text: String, parsed: String, confirmed: String) extends Action
case class LogMessageType(text: String, messageType: MessageType) extends Action

sealed trait DatabaseAction extends Action

case class TelegramAction(to: Long, text: String) extends Action

case class SaveOrUpdateUserAction(user: User) extends DatabaseAction

case class SaveOrUpdateReminderAction(reminder: Reminder) extends DatabaseAction
