package com.deeploma.environments

import com.deeploma.core.{Environment, Event, ReminderEvent}
import com.deeploma.repository.InMemoryReminderRepository

class ReminderEnvironment extends Environment {
  override def fetchEvents(): Seq[Event] = InMemoryReminderRepository.repo.findDueReminders().map(ReminderEvent)
}

object ReminderEnvironment {
  def createEnvironment(): ReminderEnvironment = new ReminderEnvironment
}