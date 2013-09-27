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
    			""
    		}
    	} ~
    	post {
    		entity(as[SignUpMessage]) { message =>
  				complete { 
  					message
  				}
    		}
    	}
    } ~
    path("confirm" / JavaUUID) { token =>
    	get {
    			complete {
    				""
    			}
    	} ~
    	post {
    		entity(as[String]) { password =>
    			complete {
    				""
    			}
    		}
    	}
    } ~
    path("requests") {
    	get {
    		complete {
    				// SQL Injection Check
    				// Check User Token
            // Get requests
    				""
    		}
  		} ~
    	post {
    		entity(as[RelationshipRequestMessage]) { message =>
  				complete {
	    			// SQL Injection Check
	    			// Check User Token
	    			""
	    		}
    		}
    	} ~
    	put {
    		entity(as[RelationshipConfirmationMessage]) { message =>
          complete {
            // SQL Injection Check
            // Check User Token
            ""
          }
        }
    	}	
    } ~
   	path("profile") {
   		entity(as[String]) { id =>
	   		get {
	    			complete {
	    				// SQL Injection Check
	    				// Check User Token
	    				// If id is user
	    					// Get self profile page
	    				// else
	    					// If users are friends
	    						// Get friend profile page
	    					// else
	    						// Reject
	    				""
	    			}
	    	}
    	}
   	} ~
   	path("friends") {
   		get {
    			complete {
    				// SQL Injection Check
    				// Check User Token
    				// Get page with friends
    				""
    			}
    	}
   	} ~
   	path("search") {
   		post {			
   			entity(as[String]) { search =>
    			complete {
    				// SQL Injeciton Check
    				// Check User Token
    				// Get page for search criteria
    				""
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
