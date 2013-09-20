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
  "com.typesafe.slick" % "slick_2.10" % "2.0.0-M2",
  "com.typesafe" % "config" % "1.0.2"
)

org.scalastyle.sbt.ScalastylePlugin.Settings
