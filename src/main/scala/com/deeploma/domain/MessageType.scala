package com.deeploma.domain

sealed trait MessageType
case object Request extends MessageType {
  override def toString: String = "request"
}

case object Question extends MessageType {
  override def toString: String = "question"
}

case object EmptyMessageType extends MessageType {
  override def toString: String = ""
}

case object Fact extends MessageType {
  override def toString: String = "fact"
}
