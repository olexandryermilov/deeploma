package com.deeploma.core

import java.util.{Date, UUID}

sealed trait Event

/**
 * abstract events
 */
case class UserEvent(userId: UUID) extends Event

/**
 * specific events
 */
case class ClockEvent(time: Date) extends Event
