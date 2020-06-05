name := "deeploma"

version := "0.1"

scalaVersion := "2.12.2"

libraryDependencies += "com.bot4s" % "telegram-core_2.12" % "4.4.0-RC2"
// https://mvnrepository.com/artifact/com.softwaremill.sttp.client/core
libraryDependencies += "com.softwaremill.sttp" %% "core" % "1.2.0"
libraryDependencies += "com.softwaremill.sttp" %% "okhttp-backend" % "1.2.0"

libraryDependencies ++= Seq("org.specs2" %% "specs2-core" % "4.6.0" % "test")
// https://mvnrepository.com/artifact/com.yahoofinance-api/YahooFinanceAPI
libraryDependencies += "com.yahoofinance-api" % "YahooFinanceAPI" % "3.15.0"


//libraryDependencies += "uk.ac.abdn" % "SimpleNLG" % "4.4.8"
// https://mvnrepository.com/artifact/com.johnsnowlabs.nlp/spark-nlp
/*libraryDependencies += "com.johnsnowlabs.nlp" % "spark-nlp_2.11" % "2.5.1"
libraryDependencies += "org.apache.spark" % "spark-core_2.11" % "2.4.0"
libraryDependencies +=  "org.apache.spark" % "spark-sql_2.11" % "2.4.0"
libraryDependencies +=  "org.apache.spark" % "spark-mllib_2.11" % "2.4.0"*/
// https://mvnrepository.com/artifact/com.mashape.unirest/unirest-java
libraryDependencies += "com.mashape.unirest" % "unirest-java" % "1.4.9"
libraryDependencies += "com.fasterxml.jackson.module" % "jackson-module-scala_2.12" % "2.9.8"
libraryDependencies += "io.lemonlabs" %% "scala-uri" % "2.2.2"