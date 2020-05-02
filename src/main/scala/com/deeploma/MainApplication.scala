package com.deeploma

import com.deeploma.core._
import com.deeploma.environments.{ClockEnvironment, TelegramEnvironment}
import com.deeploma.service.ReactionService

object MainApplication {

  def main(args: Array[String]): Unit = {
    work()
  }

  lazy val environments: Seq[Environment] = listAllEnvironments()

  def listAllEnvironments(): Seq[Environment] = Seq(
    ClockEnvironment.createEnvironment,
    TelegramEnvironment.env
  )

  def work(): Unit = {
    while (true) {
      for {
        environment <- environments
        event <- environment.fetchEvents()
        reaction <- reactToEvent(event)
      } yield doAction(reaction)
    }
  }

  def reactToEvent(event: Event): Seq[Action] = ReactionService.reactToEvent(event)

  def doAction(action: Action): Unit = action match {
    case LoggableAction(response) => println(response)
    case DatabaseAction() => ???
    case TelegramAction(to, text) => TelegramEnvironment.env.sendMessage(to, text)
  }

}
