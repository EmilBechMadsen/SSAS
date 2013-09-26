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

  // The default timeout for all requests
  implicit val timeout = Timeout(Settings.timeout)

  def actorRefFactory = context
  def receive = runRoute(route)
  def cookieName : String = "ssas_session"

  val route = 
    path("test") {
      get {
        complete {
          "Hello"
        }
      }
    } ~
    path("signup") {
    	get {
    		complete {
    			""
    		}
    	} ~
    	post {
    		entity(as[String]) { email => 
    			entity(as[String]) { password =>
    				complete {
    					email + password
    				}
    			}
    		}
    	}
    } ~
    path("confirm") { 
    	entity(as[String]) {token =>
	    	get {
	    			complete {
	    				token
	    			}
	    	} ~
	    	post {
	    		entity(as[String]) { password =>
	    			complete {
	    				password + token
	    			}
	    		}
	    	}
    	}
    } ~
    path("requests") {
    	get {
    		complete {
    				""
    		}
  		} ~
    	post {
    		entity(as[String]) { accepted =>
    			entity(as[String]) { relationship =>
    				complete {
		    			// SQL Injection Check
		    			// Check User Token
		    			""
		    		}
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
   			entity(as[String]) { email => 
    			entity(as[String]) { password =>
    				// SQL Injection Check
    				// Check password/username
    				// If match
    				setCookie(HttpCookie(cookieName, "content")) {
	    				complete {
	    					// Redirect to appropiate page
	    					""
	    				}
    				}
    				// If not match
    				// 403
    			}
    		}
   		}
   	}
}
