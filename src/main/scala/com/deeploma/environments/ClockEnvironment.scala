package com.deeploma.environments

import java.util.Date

import com.deeploma.core.{ClockEvent, Environment, Event}

class ClockEnvironment extends Environment {
  override def fetchEvents(): Seq[Event] = Seq(ClockEvent(new Date(System.currentTimeMillis())))
}

object ClockEnvironment {
  def createEnvironment = new ClockEnvironment()
}
