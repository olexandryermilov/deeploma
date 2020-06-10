package com.deeploma.service

import com.deeploma.domain.{Food, ReminderTopic, Stock, Topic, Undefined, WeatherTopic, CovidTopic}

object TopicDetector {
  def detectTopic(text: String): Topic = {
    val lowerCasedText = text.toLowerCase
    if (lowerCasedText.contains("remind")) ReminderTopic
    else if (lowerCasedText.contains("stock")) Stock
    else if (lowerCasedText.contains("calories") || lowerCasedText.contains("vitamin") || lowerCasedText.contains("carbs")) Food
      else if (lowerCasedText.contains("weather")) WeatherTopic
      else if(lowerCasedText.contains("covid")) CovidTopic
    else Undefined
  }
}
