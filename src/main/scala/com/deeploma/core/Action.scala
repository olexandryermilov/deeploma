package com.deeploma.core

import java.util.{Date, UUID}

sealed trait Action

case class EmptyAction() extends Action

case class LoggableAction(response: String) extends Action

sealed trait DatabaseAction extends Action

case class TelegramAction(to: Long, text: String) extends Action

case class SaveOrUpdateUserAction(id: UUID,
                                  telegramContext: Option[TelegramContext] = None,
                                  userContext: Option[UserContext] = None) extends DatabaseAction

case class SaveOrUpdateReminderAction(id: UUID,
                                      userId: UUID,
                                      text: String,
                                      when: Date,
                                      wasSent: Boolean) extends DatabaseAction
