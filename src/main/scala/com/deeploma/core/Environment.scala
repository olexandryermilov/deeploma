package com.deeploma.core

trait Environment {
  def fetchEvents(): Seq[Event]
}
