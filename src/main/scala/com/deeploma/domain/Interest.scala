package com.deeploma.domain

sealed trait Interest

case class StockInterest(name: String) extends Interest