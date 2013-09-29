package dk.itu.ssas

import akka.actor.{Actor, ActorLogging, ActorRef, Props}

import spray.routing._
import spray.http._
import spray.httpx.SprayJsonSupport

/** Exposes webservices provided by clireSkies */
class Service
  extends Actor
  with HttpService
  with SprayJsonSupport 
  with ActorLogging {

  import akka.pattern.ask
  import akka.util.Timeout
  import java.util.UUID
  import scala.concurrent.Await
  import dk.itu.ssas.model._

  // The default timeout for all requests
  implicit val timeout = Timeout(Settings.timeout)

  def actorRefFactory = context
  def receive = runRoute(route)
  val cookieName : String = "ssas_session"

  import dk.itu.ssas.SSASMessageProtocol._

  val route = 
    path("signup") { 
    	get {
    		complete {
          // Get sign up page
    			""
    		}
    	} ~
    	post {
    		entity(as[SignUpMessage]) { message =>
  				complete { 
  					// Create appropiate sign up data
            // Redirect to appropiate site
            ""
  				}
    		}
    	}
    } ~
    path("confirm" / JavaUUID) { token =>
    	get {
    			complete {
            // Check confirmation token validity 
    				// Get confirmation formular
            ""
    			}
    	} ~
    	post {
    		entity(as[String]) { password =>
    			complete {
    				// Create appropiate user data
            // Redirect to appropiate site
            ""
    			}
    		}
    	}
    } ~
    path("requests") {
    	get {
    		cookie(cookieName) { c => r =>
            val user = User(UUID.fromString(c.content))
            user match {
              case Some(u) =>
                complete {
                  //
                  ""
                }
              case None =>
                // Reject
                HttpResponse(spray.http.StatusCodes.Unauthorized, "Session was invalid.")
            }
          }
  		} ~
    	post {
    		entity(as[RelationshipRequestMessage]) { message =>
  				cookie(cookieName) { c => r =>
            val user = User(UUID.fromString(c.content))
            user match {
              case Some(u) =>
                complete {
                  //
                  ""
                }
              case None =>
                // Reject
                HttpResponse(spray.http.StatusCodes.Unauthorized, "Session was invalid.")
            }
          }
    		}
    	} ~
    	put {
    		entity(as[RelationshipConfirmationMessage]) { message =>
          cookie(cookieName) { c => r =>
            val user = User(UUID.fromString(c.content))
            user match {
              case Some(u) =>
                complete {
                  //
                  ""
                }
              case None =>
                // Reject
                HttpResponse(spray.http.StatusCodes.Unauthorized, "Session was invalid.")
            }
          }
        }
    	}	
    } ~
   	path("profile" / IntNumber) { id =>
   		get {
        cookie(cookieName) { c => r =>
          val user = User(UUID.fromString(c.content))
          user match {
            case Some(u) =>
              complete {
                if (u.id == id) {
                  // Get own ID page
                  ""
                } else {
                  // Get requested ID page
                  ""
                }
              }
            case None =>
              // Reject
              HttpResponse(spray.http.StatusCodes.Unauthorized, "Session was invalid.")
          }
        }
    	}
   	} ~
   	path("friends") {
   		get {
    		cookie(cookieName) { c => r =>
          val user = User(UUID.fromString(c.content))
          user match {
            case Some(u) =>
              complete {
                // Get 
                ""
              }
            case None =>
              // Reject
              HttpResponse(spray.http.StatusCodes.Unauthorized, "Session was invalid.")
          }
        }
    	}
   	} ~
   	path("search") {
      entity(as[String]) { search =>
     		post {			
    			cookie(cookieName) { c => r =>
            val user = User(UUID.fromString(c.content))
            user match {
              case Some(u) =>
                complete {
                  // Get search page
                  ""
                }
              case None =>
                // Reject
                HttpResponse(spray.http.StatusCodes.Unauthorized, "Session was invalid.")
            }
          }
      	}
      }
   	}
   	path("login") {
   		post {
   			entity(as[LogInMessage]) { message => 
  				val user = User.login(message.email, message.password) 
          user match {
            case Some(u) => 
              // Login was successful
              val session = u.session
              session match {
                case Some(s) =>
                  setCookie(HttpCookie(cookieName, s.toString())) {
                    complete {
                      // TODO: Redirect to appropiate page
                      ""
                    }
                  }
                case None =>
                  complete {
                    // Session did not exist.
                    HttpResponse(spray.http.StatusCodes.Unauthorized, "Session was invalid.")
                  }
              }
              
            case None =>
              // Login was not successful.
              complete {
                // Reject
                HttpResponse(spray.http.StatusCodes.Unauthorized, "User or password was incorrect.")
              }
    			}
    		}
   		}
   	}
}
