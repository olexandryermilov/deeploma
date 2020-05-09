package com.deeploma.repository

import java.util.UUID

import com.deeploma.domain.Reminder

class InMemoryReminderRepository extends ReminderRepository {

  var storage: Seq[Reminder] = Seq.empty

  override def saveOrUpdateReminder(reminder: Reminder): Unit = storage = storage.filter(_.id != reminder.id) ++ Seq(reminder)

  override def findReminderByReminderId(reminderId: UUID): Option[Reminder] = storage.find(_.id == reminderId)

  override def findReminderByUserId(userId: UUID): Option[Reminder] = storage.find(_.userId == userId)

  override def findDueReminders(): Seq[Reminder] =
    storage.filter(reminder => !reminder.wasSent && reminder.time.getTime < System.currentTimeMillis())

}

object InMemoryReminderRepository {
  lazy val repo = new InMemoryReminderRepository()

  def repository: InMemoryReminderRepository = repo

}
