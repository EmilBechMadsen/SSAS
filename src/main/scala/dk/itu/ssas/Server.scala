package dk.itu.ssas

import dk.itu.ssas.db.DbAccess

object Server extends App with DbAccess {
  import akka.actor.{ActorSystem, Props}
  import akka.io.IO
  import scala.language.postfixOps
  import scala.slick.jdbc.{ GetResult, StaticQuery => Q }
  import scala.slick.driver.MySQLDriver.simple._
  import scala.slick.driver.MySQLDriver.simple.Database.threadLocalSession
  import spray.can.Http

  implicit val system = ActorSystem("ssas")

  // create and start our service actor
  val service  = system.actorOf(Props[Service], "service")
  val mailer   = system.actorOf(Props[MailSender], "mailer")
  val dbWorker = system.actorOf(Props[DbWorker], "dbWorker")

  // create database if it doesn't exists
  if (Settings.db.dbUser == "root") {
    Db withSession {
      val createDatabase = Q.u + "CREATE DATABASE IF NOT EXISTS ssas;"
      createDatabase.execute()

      if (Q.queryNA[String]("SHOW TABLES LIKE 'user'").list.isEmpty) {
        ddl create
      }
      //ddl.createStatements foreach {s => println(s"$s;")}
    }
  }

  // create a new HttpServer using our handler and tell it where to bind to
  IO(Http) ! Http.Bind(service, Settings.interface, Settings.port)
}
