package dk.itu.ssas

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import dk.itu.ssas.services._
import spray.routing._
import spray.http._
import spray.httpx.SprayJsonSupport

/** Exposes webservices provided by clireSkies */
class Service
  extends Actor
  with HttpService
  with UserService
  with SprayJsonSupport {

  import akka.util.Timeout
  
  // The default timeout for all requests
  implicit val timeout = Timeout(Settings.timeout)

  def actorRefFactory = context
  def receive = runRoute(route)

  def redirects = 
    path("") {
      get {
        redirect("/signup", StatusCodes.Found)
      }
    } ~
    path("ssase13") {
      get {
        redirect("/", StatusCodes.MovedPermanently)
      }
    }

  val route = {
      redirects ~
      PublicService.route ~ 
      userRoute ~ 
      AdminService.route ~
      ApiService.route  
   }
 }
