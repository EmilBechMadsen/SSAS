package dk.itu.ssas

import dk.itu.ssas.db.DbAccess

object Server extends App with SsasSslConfiguration with DbAccess {
  import akka.actor.{ActorSystem, Props}
  import akka.io.IO
  import scala.language.postfixOps
  import spray.can.Http

  implicit val system = ActorSystem("ssas")

  // create and start our service actor
  val service = system.actorOf(Props[Service], "service")

  // create a new HttpServer using our handler and tell it where to bind to
  IO(Http) ! Http.Bind(service, Settings.interface, Settings.port)
}
