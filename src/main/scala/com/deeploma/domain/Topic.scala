package com.deeploma.domain

sealed trait Topic
case object Food extends Topic
case object Stock extends Topic
case object ReminderTopic extends Topic
case object Undefined extends Topic
case object WeatherTopic extends Topic
case object CovidTopic extends Topic
