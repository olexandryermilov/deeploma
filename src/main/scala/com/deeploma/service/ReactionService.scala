package com.deeploma.service

import java.text.SimpleDateFormat
import java.util.{Date, UUID}

import com.deeploma.core._
import com.deeploma.domain.{EmptyMessageType, Fact, Food, MessageType, Question, Reminder, ReminderTopic, Request, Stock, StockInterest, Undefined, User}
import com.deeploma.repository.{InMemoryReminderRepository, InMemoryUserRepository}
import com.deeploma.utils._
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.mashape.unirest.http.Unirest
import io.lemonlabs.uri.Url
import yahoofinance.YahooFinance

import scala.util.Try

object ReactionService {

  def reactToEvent(event: Event): Seq[Action] = event match {
    case event@ClockEvent(_) => clockEvent(event)
    case event@TelegramEvent(_) => telegramEvent(event)
    case event@ReminderEvent(_) => reminderEvent(event)
    case event@_ => unknownEvent(event)
  }

  def checkForEmptyTelegramResponses(events: Seq[Event], reactions: Seq[Action]): Seq[Action] =
    events
      .filter(event => event.isInstanceOf[TelegramEvent])
      .filterNot(event => reactions
        .filter(reaction => reaction.isInstanceOf[TelegramAction])
        .exists(reaction => reaction.asInstanceOf[TelegramAction].to == event.asInstanceOf[TelegramEvent].message.chat.id)
      ).map(event => {
      NoResponseLogger.handleNoResponseEvent(event)
      findMessageForConversation(event.asInstanceOf[TelegramEvent])
    })

  private def findMessageForConversation(event: TelegramEvent): Action = {
    val user: User = getUserByTelegramChatIdUnsafe(event.chatId)
    val sentimentOfMessage = sentimentAnalysisScore(event.text)
    if(user.meanSentimentScore >= 0.5 || sentimentOfMessage > 0.3){
      TelegramAction(event.chatId, "Good to see that you are in a good mood!")
    }
      else if(user.meanSentimentScore<= -0.5 || sentimentOfMessage < -0.3){
      TelegramAction(event.chatId, "Hey, are you alright? You sound sad today!")
    } else sorryTelegramEvent(event.asInstanceOf[TelegramEvent].message.chat.id)
  }

  def checkForUnloggedMessages(events: Seq[Event], reactions: Seq[Action]): Seq[Action] =
    events.filter(event => event.isInstanceOf[TelegramEvent])
      .filterNot(_ => reactions.exists(reaction => reaction.isInstanceOf[LogMessageTypeAction])
      ).map(event => {
      LogMessageTypeAction(event.asInstanceOf[TelegramEvent].text, EmptyMessageType)
    })

  private def clockEvent(clockEvent: ClockEvent): Seq[Action] = Seq(
    //LoggableAction(response = new SimpleDateFormat("dd/MM/yyyy hh/mm/ss").format(clockEvent.time))
  )

  private def telegramEvent(event: TelegramEvent): Seq[Action] = {
    val chatId: Long = event.chatId
    val maybeUser: Option[User] = getUserByTelegramChatId(chatId)
    val textMessage: String = event.text
    val debug = if (textMessage == "gdpr") Seq(TelegramAction(to = chatId, maybeUser.get.toString)) else Seq.empty
    val userActions: Seq[Action] =
      if (maybeUser.isEmpty)
        Seq(
          TelegramAction(to = chatId, text = askForName),
          SaveOrUpdateUserAction(User(id = UUID.randomUUID(), telegramContext = Some(TelegramContext(chatId = chatId, allMessages = Seq(textMessage), lastActionDone = Some(TelegramAction(to = chatId, text = askForName))))))
        )
      else {
        val user: User = maybeUser.get
        val contextActions: Seq[Action] = parseContext(user, textMessage)
        val result = if (contextActions.nonEmpty) contextActions
        else {
          val messageType = checkSentenceType(event)
          messageType match {
            case Request =>
              val topic = TopicDetector.detectTopic(textMessage)
              topic match {
                case Food => reactToFoodQuestion(event)
                case Stock => parseStockRequest(event)
                case ReminderTopic => parseRemindRequest(event)
                case Undefined => Seq(LogMessageTypeAction(event.text, Fact))
              }
            case Question => answerQuestion(event) ++ Seq(LogMessageTypeAction(event.text, Question))
          }

        }
        result
      }
    userActions ++ debug ++ Seq(UpdateMessageHistoryAction(chatId, textMessage)) ++ sentimentAnalysis(event)
  }

  private def unknownEvent(event: Event): Seq[Action] = Seq.empty

  private def describeMessage(text: String) = {
    /*val testData = spark.createDataFrame(Seq(
      (1, "Google has announced the release of a beta version of the popular TensorFlow machine learning library"),
      (2, "Donald John Trump (born June 14, 1946) is the 45th and current president of the United States")
    )).toDF("id", "text")

    val pipeline = PretrainedPipeline("explain_document_dl", lang="en")

    val annotation = pipeline.transform(testData)*/
  }

  private def parseStockRequest(event: TelegramEvent): Seq[Action] = {
    val text: String = event.text.toLowerCase
    if (text.contains("stock")) {
      val chatId: Long = event.chatId
      val user: User = getUserByTelegramChatIdUnsafe(chatId)
      val stockName = askBert("What stock am I talking about", text)
      val maybeStock = Try {
        YahooFinance.get(stockName.toUpperCase)
      }.toOption.nullToNone
      if (maybeStock.nonEmpty) {
        val stock = maybeStock.get
        val price = stock.getQuote.getPrice
        val stockInterest = StockInterest(stockName)
        (if (!user.interests.contains(stockInterest)) Seq(SaveOrUpdateUserAction(user.withNewInterest(stockInterest).withNewMessage(event.text))) else Seq.empty) ++ Seq(
          TelegramAction(chatId, s"$stockName is currently selling for ${price.toString}."),
          LogMessageTypeAction(text, Request),
        )
      }
      else {
        Seq(
          TelegramAction(chatId, "Sorry, I couldn't find this stock you are talking about. Maybe it's not in Yahoo's base yet or you need to check your spelling?")
        )
      }
    }
    else Seq.empty
  }

  private def parseRemindRequest(event: TelegramEvent): Seq[Action] = {
    val text: String = event.text.toLowerCase
    if (text.contains("remind")) {
      val chatId: Long = event.chatId
      val user: User = getUserByTelegramChatIdUnsafe(chatId)
      val whenDate: Date = new Date(parseTimeForReminder(text) + System.currentTimeMillis())
      val when: String = new SimpleDateFormat(dateFormat).format(whenDate)
      val what: String = askBert(text, "What should I remind you?")
      val confirmReminder: TelegramAction = TelegramAction(to = chatId, text = s"${user.name}, you're asking to remind you at $when to $what, right?")
      Seq(
        confirmReminder,
        SaveOrUpdateReminderAction(Reminder(UUID.randomUUID(), user.id, text, whenDate, wasSent = false, wasConfirmed = false)),
        LogMessageTypeAction(text, Request)
      )
    } else
      Seq.empty
  }

  private def sorryTelegramEvent(chatId: Long): Action = {
    val userName = getUserByTelegramChatId(chatId).map(_.name).getOrElse("")
    TelegramAction(
      chatId,
      sorryNoAnswer.replace("{name}", userName)
    )
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  case class FoodResp(answer: String)

  private def reactToFoodQuestion(event: TelegramEvent): Seq[Action] = Try {
    val response = Unirest.get(s"https://spoonacular-recipe-food-nutrition-v1.p.rapidapi.com/recipes/quickAnswer?q={q}")
      .header("x-rapidapi-host", "spoonacular-recipe-food-nutrition-v1.p.rapidapi.com")
      .header("x-rapidapi-key", "8265e9934bmsh830223de097c707p107c33jsncdf2b74dfb6b")
      .routeParam("q", event.text)
      .asString().getBody

    val answer = Try(mapper.readValue[FoodResp](response).answer.nullToFailed).getOrElse("")

    Seq(TelegramAction(event.chatId, text = answer), LogMessageTypeAction(event.text, Request))
  }.map {
    case e: Throwable => Seq(TelegramAction(event.chatId, text = e.getMessage))
    case x@_ => x
  }.get

  private def reminderEvent(event: ReminderEvent): Seq[Action] = {
    val user = getUserByUserIdUnsafe(event.reminder.userId)
    Seq(
      user.telegramContext.map(context => TelegramAction(
        to = context.chatId,
        text = event.reminder.text
      )).getOrElse(EmptyAction()),
      SaveOrUpdateReminderAction(
        event.reminder.copy(
          wasSent = true
        ))
    )
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  case class BertResp(answer: String)

  private def answerQuestion(event: TelegramEvent): Seq[Action] = {
    val question = event.text
    if (isAQuestion(question)) {
      val userMessages = getUserByTelegramChatIdUnsafe(event.chatId).telegramContext.get.allMessages.mkString(". ")

      Seq(TelegramAction(event.chatId, text = askBert(question, userMessages)))
    } else Seq.empty
  }

  private def isAQuestion(text: String): Boolean = text.contains("?")

  private def askBert(question: String, text: String): String = {
    val body =
      s"""{
         |"text":"$text",
         |"question":"$question"
         |}
         |""".stripMargin
    val request = Unirest.post(s"http://127.0.0.1:9090/bert")
      .header("Content-Type", "application/json")
      .body(body)
    val response = request.asString().getBody
    Try(mapper.readValue[BertResp](response).answer).getOrElse("")
  }

  private def checkSentenceType(event: TelegramEvent): MessageType = {
    val text = event.text
    if (text.contains("?")) Question
    else Request
  }

  private def parseContext(user: User, textMessage: String): Seq[Action] = {
    user.telegramContext match {
      case Some(context) => context.lastActionDone match {
        case Some(TelegramAction(id, text)) if text == askForName =>
          val name = textMessage
          val niceToMeetYou = TelegramAction(id, s"Hi, $name! Nice to meet you!")
          Seq(
            SaveOrUpdateUserAction(User(id = user.id, telegramContext = Some(TelegramContext(id, Some(niceToMeetYou), allMessages = Seq(s"My name is $name"))), userContext = Some(UserContext(name, Seq.empty, Seq.empty)))),
            niceToMeetYou
          )
        case Some(TelegramAction(id, _)) if textMessage.toLowerCase == "forget about it" => Seq(
          TelegramAction(id, "Ok, done")
        )
        case Some(TelegramAction(id, text)) if text.contains("you're asking to remind you at") =>
          val reminder = InMemoryReminderRepository.repo.findReminderByUserId(user.id).get
          if (textMessage.toLowerCase.contains("yes") || textMessage.toLowerCase.contains("right")) {
            Seq(
              SaveOrUpdateReminderAction(reminder.copy(wasConfirmed = true)),
              LogReminderConfirmationAction(text, reminder.text, reminder.text),
              TelegramAction(id, s"Ok, ${user.name}, I'll create a reminder for you"),
            )
          } else
            Seq(
              TelegramAction(id, s"Ok, ${user.name}, then what do you want me to remind you about at ${new SimpleDateFormat(dateFormat).format(reminder.time)}?"),
            )
        case Some(TelegramAction(id, text)) if text.contains("then what do you want me to remind you about") =>
          val reminder = InMemoryReminderRepository.repo.findReminderByUserId(user.id).get
          Seq(
            TelegramAction(id, "Ok, I'll create a reminder for you!"),
            LogReminderConfirmationAction(reminder.text, reminder.text, textMessage),
            SaveOrUpdateReminderAction(reminder.copy(wasConfirmed = true, text = textMessage))
          )
        case _ => Seq.empty
      }
      case None => Seq.empty
    }
  }

  private def sentimentAnalysisScore(text: String): Double = Try {
    val response = Unirest.post("https://twinword-sentiment-analysis.p.rapidapi.com/analyze/")
      .header("x-rapidapi-host", "twinword-sentiment-analysis.p.rapidapi.com")
      .header("x-rapidapi-key", "8265e9934bmsh830223de097c707p107c33jsncdf2b74dfb6b")
      .header("content-type", "application/x-www-form-urlencoded")
      .body(
        s"text=${Url.parse(text)}")
      .asString().getBody
    Try(mapper.readValue[SentimentResp](response).score.nullToFailed).getOrElse(0.0)
  }.get

  private def sentimentAnalysis(event: TelegramEvent): Seq[Action] = Try {
    val response = Unirest.post("https://twinword-sentiment-analysis.p.rapidapi.com/analyze/")
      .header("x-rapidapi-host", "twinword-sentiment-analysis.p.rapidapi.com")
      .header("x-rapidapi-key", "8265e9934bmsh830223de097c707p107c33jsncdf2b74dfb6b")
      .header("content-type", "application/x-www-form-urlencoded")
      .body(
        s"text=${Url.parse(event.text)}")
      .asString().getBody
    println(Url.parse(event.text))
    println(response)
    val answer = Try(mapper.readValue[SentimentResp](response).score.nullToFailed).getOrElse(0.0)
    Seq(UpdateScoreHistoryAction(event.chatId, answer))
  }.map {
    case e: Throwable => Seq(TelegramAction(event.chatId, text = e.getMessage))
    case x@_ => x
  }.get

  @JsonIgnoreProperties(ignoreUnknown = true)
  case class SentimentResp(score: Double)


  private def parseTimeForReminder(text: String): Long = {
    val timeMeasures = Seq("seconds", "minutes", "hours", "second", "minute", "hour")
    val words = text.split(" ")
    timeMeasures.map(measurement => {
      val measurementIndex = words.indexOf(measurement)
      if (measurementIndex > 0) words(measurementIndex - 1).toInt * measurementToMilliSeconds(measurement)
      else 0
    }).sum
  }

  private def measurementToMilliSeconds(measurement: String): Int =
    if (measurement.contains("second")) 1000
    else if (measurement.contains("minute")) 1000 * 60
    else if (measurement.contains("hour")) 1000 * 60 * 60
    else 0

  private def getUserByTelegramChatId(chatId: Long): Option[User] = InMemoryUserRepository.repository.getUserByTelegramChatId(chatId)

  private def getUserByTelegramChatIdUnsafe(chatId: Long): User = getUserByTelegramChatId(chatId).get

  private def getUserByUserId(userId: UUID): Option[User] = InMemoryUserRepository.repository.getUser(userId)

  private def getUserByUserIdUnsafe(userId: UUID): User = getUserByUserId(userId).get

  val askForName = "Hi, I don't know you, please, tell me your name"
  val sorryNoAnswer = "Sorry {name}, I don't know yet how to respond to your message. But I'm still learning"
  val dateFormat = "MM-dd-yyyy HH:mm:ss"

  lazy val mapper = new ObjectMapper() with ScalaObjectMapper

}
