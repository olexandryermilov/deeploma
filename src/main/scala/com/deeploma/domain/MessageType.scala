package com.deeploma.domain

sealed trait MessageType
case object Request extends MessageType {
  override def toString: String = "request"
}
