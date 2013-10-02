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
  					val user = User.create(message.name, None, message.email, message.password)
            user match {
              case Some(u) =>
                // User created
                ""
              case None =>
                // User not created
                ""
            }   
  				}
    		}
    	}
    } ~
    path("confirm" / JavaUUID) { token =>
    	get {
    			complete {
            val user = User(token)
            user match {
              case Some(u) =>
                // User exists.
                // Get page
                ""
              case None =>
                // No token found for user
                ""                
            }
          }
    	} ~
    	post {
    		entity(as[String]) { password =>
    			complete {
    				val user = User(token)
            user match {
              case Some(u) =>
                if(u.checkPassword(password)) 
                  if(u.confirm(token)) 
                    "" // Success
                  else
                  HttpResponse(spray.http.StatusCodes.BadRequest, "Confirmation id was invalid.")                  
                else 
                  HttpResponse(spray.http.StatusCodes.Unauthorized, "User or password was incorrect.")
              case None =>
                HttpResponse(spray.http.StatusCodes.Unauthorized, "Session was invalid.")
                
            }
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
                  val requests = u.friendRequests
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
                  val otherUser = User(message.userId)
                  otherUser match {
                    case Some(ou) =>
                      u.requestFriendship(ou, message.rel)
                    case None =>
                      HttpResponse(spray.http.StatusCodes.BadRequest, "User not found.")      
                  }
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
                  val otherUser = User(message.userId)
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
                  var users = User.search(search)
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
    path("logout") {
      post {      
          cookie(cookieName) { c => r =>
            val user = User(UUID.fromString(c.content))
            user match {
              case Some(u) =>
                complete {
                  u.logout()
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
