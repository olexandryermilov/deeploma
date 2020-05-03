name := "deeploma"

version := "0.1"

scalaVersion := "2.12.2"

libraryDependencies += "com.bot4s" % "telegram-core_2.12" % "4.4.0-RC2"
// https://mvnrepository.com/artifact/com.softwaremill.sttp.client/core
libraryDependencies += "com.softwaremill.sttp" %% "core" % "1.2.0"
libraryDependencies += "com.softwaremill.sttp" %% "okhttp-backend" % "1.2.0"

libraryDependencies ++= Seq("org.specs2" %% "specs2-core" % "4.6.0" % "test")