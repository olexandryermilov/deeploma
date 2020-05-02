package com.deeploma.core

sealed trait Action

case class LoggableAction(response: String) extends Action
case class DatabaseAction() extends Action
case class TelegramAction() extends Action
