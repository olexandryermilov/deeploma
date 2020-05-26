package com.deeploma.service

import com.deeploma.domain.{Food, ReminderTopic, Stock, Topic, Undefined}

object TopicDetector {
  def detectTopic(text: String): Topic = {
    val lowerCasedText = text.toLowerCase
    if (lowerCasedText.contains("remind")) ReminderTopic
    else if (lowerCasedText.contains("stock")) Stock
    else if (lowerCasedText.contains("calories")) Food
    else Undefined
  }
}
