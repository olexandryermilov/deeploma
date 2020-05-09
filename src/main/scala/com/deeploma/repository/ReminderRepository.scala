package com.deeploma.repository

import java.util.UUID

import com.deeploma.domain.Reminder

trait ReminderRepository {

  def saveOrUpdateReminder(reminder: Reminder): Unit

  def findReminderByReminderId(reminderId: UUID): Option[Reminder]

  def findReminderByUserId(userId: UUID): Option[Reminder]

  def findDueReminders(): Seq[Reminder]

}
