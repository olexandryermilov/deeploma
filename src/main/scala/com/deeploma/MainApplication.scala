package com.deeploma

import java.text.SimpleDateFormat

import com.deeploma.core.{Action, ClockEvent, DatabaseAction, Environment, Event, LoggableAction, TelegramAction}
import com.deeploma.environments.ClockEnvironment

object MainApplication {

  def main(args: Array[String]): Unit = {
    work()
  }

  lazy val environments: Seq[Environment] = listAllEnvironments()

  def listAllEnvironments(): Seq[Environment] = Seq(
    ClockEnvironment.createEnvironment
  )

  def work(): Unit = {
    while (true) {
      environments.flatMap(_.fetchEvents()).map(reactToEvent).foreach(doAction)
    }
  }

  def reactToEvent(event: Event): Action = event match {
    case ClockEvent(time) => LoggableAction(response = new SimpleDateFormat("dd/MM/yyyy hh/mm/ss").format(time))
  }

  def doAction(action: Action): Unit = action match {
    case LoggableAction(response) => println(response)
    case DatabaseAction() => ???
    case TelegramAction() => ???
  }

}
