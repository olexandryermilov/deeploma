package com.deeploma.core

import java.util.{Date, UUID}

import com.deeploma.domain.{Reminder, User}

sealed trait Action

case class EmptyAction() extends Action

case class LoggableAction(response: String) extends Action

sealed trait DatabaseAction extends Action

case class TelegramAction(to: Long, text: String) extends Action

case class SaveOrUpdateUserAction(user: User) extends DatabaseAction

case class SaveOrUpdateReminderAction(reminder: Reminder) extends DatabaseAction
