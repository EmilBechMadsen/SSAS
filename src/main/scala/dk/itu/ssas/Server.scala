package dk.itu.ssas

object Server extends App with SsasSslConfiguration with dk.itu.ssas.db.DbModels {
  import akka.actor.{ActorSystem, Props}
  import akka.io.IO
  import scala.language.postfixOps
  import scala.slick.driver.MySQLDriver.simple._
  import scala.slick.driver.MySQLDriver.simple.Database.threadLocalSession
  import spray.can.Http

  implicit val system = ActorSystem("ssas")

  // create and start our service actor
  val service = system.actorOf(Props[Service], "service")

  // create a new HttpServer using our handler and tell it where to bind to
  //IO(Http) ! Http.Bind(service, Settings.interface, Settings.port)

  Database.forURL(Settings.dbString, driver = Settings.dbDriver) withSession {
    ddl create
  }

  import dk.itu.ssas.model.User

  val user = User.create("Christian Harrington", None, "christian@harrington.dk", "test1")
  user match {
    case None => {}
    case Some(u) => {
      println(u)
    }
  }
}
