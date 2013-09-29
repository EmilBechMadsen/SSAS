name := "ssas"

organization := "dk.itu"
 
version := "0.1"
 
scalaVersion := "2.10.2"

scalacOptions += "-deprecation"

scalacOptions += "-feature"
 
resolvers ++= Seq(
  "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
  "spray repo" at "http://repo.spray.io"
)
 
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.1.4",
  "io.spray" % "spray-can" % "1.1-M8",
  "io.spray" % "spray-routing" % "1.1-M8",
  "io.spray" % "spray-io" % "1.1-M8",
  "io.spray" %% "spray-json" % "1.2.5",
  "org.mindrot" % "jbcrypt" % "0.3m",
  "mysql" % "mysql-connector-java" % "5.1.26",
  "com.typesafe.slick" % "slick_2.10" % "1.0.1",
  "org.slf4j" % "slf4j-log4j12" % "1.7.5",
  "com.typesafe" % "config" % "1.0.2",
  "commons-validator" % "commons-validator" % "1.4.0",
  "org.scalatest" % "scalatest_2.10" % "1.9.2" % "test"
)

org.scalastyle.sbt.ScalastylePlugin.Settings
