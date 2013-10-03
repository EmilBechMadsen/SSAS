package dk.itu.ssas

import akka.actor.{Actor, ActorLogging, ActorRef, Props}

import spray.routing._
import spray.http._
import spray.httpx.SprayJsonSupport
import dk.itu.ssas.model.UserExceptions

/** Exposes webservices provided by clireSkies */
class Service
  extends Actor
  with HttpService
  with SprayJsonSupport 
  with ActorLogging 
  with UserExceptions {

  import akka.pattern.ask
  import akka.util.Timeout
  import java.util.UUID
  import scala.concurrent.Await
  import spray.http.MediaType
  

  // The default timeout for all requests
  implicit val timeout = Timeout(Settings.timeout)

  def actorRefFactory = context
  def receive = runRoute(route)
  val sessionCookieName : String = "ssas_session"
  val formkeyCookieName : String = "ssas_key"

  import dk.itu.ssas.SSASMessageProtocol._
  import dk.itu.ssas.Validate._
  import dk.itu.ssas.model._
  import dk.itu.ssas.page._
  import dk.itu.ssas.page.request._

  private def renewFormKey(c: String => RequestContext => Unit): RequestContext => Unit = {
    val formKey = UUID.randomUUID().toString()
    setCookie(HttpCookie(formkeyCookieName, formKey)) {
      c(formKey)
    }
  }

  private def postWithFormKey(c: RequestContext => Unit): RequestContext => Unit = {
    post {
      cookie(formkeyCookieName) { cookieKey =>
        formField('formkey) { formKey =>
          if (cookieKey.content == formKey) {
            c
          }
          else {
            complete {
              HttpResponse(StatusCodes.Unauthorized, "XSRF protection kicked in. Please try again.")
            }
          }
        }
      }
    }
  }

  private def withUser(c: User => RequestContext => Unit): RequestContext => Unit = {
    cookie(sessionCookieName) { sessionCookie =>
      try {
        val id = UUID.fromString(sessionCookie.content) 
        User(id) match {
          case Some(user) => c(user)
          case None => complete {
            HttpResponse(StatusCodes.Unauthorized, "You need to be logged in to access this page")
          }
        }
      } catch {
        case e: IllegalArgumentException => complete {
          HttpResponse(StatusCodes.InternalServerError, "Could not deserialize session")
        }
      }
    }
  }

  private def html(c: String => RequestContext => Unit): RequestContext => Unit = {
    respondWithMediaType(MediaTypes.`text/html`) {
      renewFormKey { key =>
        c(key)
      }
    }
  }

  val route =
    path("signup") { 
      get {
        html { formKey =>
          complete {
            SignupPage.render("Sign up", formKey, None, NoRequest())
          }
        }
      } ~
      postWithFormKey {
        formFields('signupEmail, 'signupName, 'signupPassword, 'signupPasswordConfirm) {
          (email, name, pass1, pass2) =>
          if (pass1 == pass2 && validEmail(email) && validName(name) && validPassword(pass1)) {
            User.create(name, None, email, pass1) match {
              case Some(u) => redirect(s"/profile/${u.id}", StatusCodes.SeeOther)
              case None    => complete {
                HttpResponse(StatusCodes.InternalServerError)
              }
            } 
          } else complete {
            HttpResponse(StatusCodes.BadRequest, "Invalid information.")
          }
        }
      }
    } ~
    path("confirm" / JavaUUID) { token =>
      get {
        User(token) match {
          case Some(user) => html { formKey =>
            complete {
              EmailConfirmationPage.render("Confirm account", formKey, None, EmailConfirmationPageRequest(token))
            }
          }
          case None => complete {
            HttpResponse(StatusCodes.BadRequest, "Confirmation id was invalid.")
          }
        }
      } ~
      postWithFormKey {
        formFields('emailConfirmationPassword) { password =>
          User(token) match {
            case Some(user) => {
              if (user.checkPassword(password)) {
                user.confirm(token)
                redirect(s"/profile/${user.id}", StatusCodes.SeeOther)
              } else complete {
                HttpResponse(StatusCodes.Unauthorized, "Incorrect username or password.")
              }
            }
            case None => complete {
              HttpResponse(StatusCodes.BadRequest, "Confirmation id was invalid.")
            } 
          }
        }
      }
    } ~
    withUser { u =>
      userRoutes(u)
    }

  def userRoutes(u: User): RequestContext => Unit = {
    path("requests") {
      get {
        cookie(sessionCookieName) { c => r =>
          val user = User(UUID.fromString(c.content))
          val formKey = UUID.randomUUID().toString()
          setCookie(HttpCookie(formkeyCookieName, formKey)) {
            user match {
              case Some(u) =>
                complete {
                  val requests = u.friendRequests
                  ""
                }
              case None =>
                // Reject
                complete { HttpResponse(spray.http.StatusCodes.Unauthorized, "Session was invalid.") }
            }
          }
        }
      } ~
      post {
        entity(as[RelationshipRequestMessage]) { message =>
          cookie(sessionCookieName) { c => r =>
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
          cookie(sessionCookieName) { c => r =>
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
        cookie(sessionCookieName) { c => r =>
          val user = User(UUID.fromString(c.content))
          val formKey = UUID.randomUUID().toString()
          setCookie(HttpCookie(formkeyCookieName, formKey)) {
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
                complete { HttpResponse(spray.http.StatusCodes.Unauthorized, "Session was invalid.") }
                
            }
          }
        }
      } ~
      post {
        cookie(sessionCookieName) { sessionC => _ =>
          cookie(formkeyCookieName) { sessionK => _ =>
            val user = User(UUID.fromString(sessionC.content))
            user match {
              case Some(u) =>
                formFields('formkey, 'profileName, 'profileAddress, 'profileCurrentPassword, 'profileNewPassword, 'profileNewPasswordConfirm) { 
                  (formkey, name, address, cPassword, nPassword, nPasswordConf) =>

                  if(formkey == sessionK.content) {
                    if(u.checkPassword(cPassword)) {

                      (validName(name), validAddress(Some(address)), (nPassword == nPasswordConf)) match {
                        case (true, true, true) =>
                          try {
                            if(!nPassword.isEmpty) {
                              (validPassword(nPassword), validPassword(nPasswordConf)) match {
                                case (true, true) =>
                                  u.password = nPassword
                                case(_, _) =>
                                  HttpResponse(spray.http.StatusCodes.BadRequest, "Not valid input.")
                              }
                            }

                            u.name = name
                            u.address = Some(address)
                            complete {
                              // Get new profile page
                              ""
                            }
                          
                          } catch {
                            case dbe: DbError       => complete { HttpResponse(spray.http.StatusCodes.InternalServerError, s"Database not accessible: $dbe.s") }
                            case ue:  UserException => complete { HttpResponse(spray.http.StatusCodes.Unauthorized, s"Not valid input: $ue.s") }
                          }

                        case (_,_,_) => 
                          complete { HttpResponse(spray.http.StatusCodes.BadRequest, "Not valid input.") }
                        }
                    } else {
                      complete { HttpResponse(spray.http.StatusCodes.Unauthorized, "Wrong password.") }
                    }
                  } else {
                    complete { HttpResponse(spray.http.StatusCodes.Unauthorized, "Form key violation.") }
                  }
              }
              case None =>
                complete { HttpResponse(spray.http.StatusCodes.Unauthorized, "Session was invalid.") }
            }
          }
        }
      }
    } ~
    path("friends") {
      get {
        cookie(sessionCookieName) { c => r =>
          val user = User(UUID.fromString(c.content))
          val formKey = UUID.randomUUID().toString()
          setCookie(HttpCookie(formkeyCookieName, formKey)) {
            user match {
              case Some(u) =>
                complete {
                  // Get 
                  ""
                }
              case None =>
                // Reject
                complete { HttpResponse(spray.http.StatusCodes.Unauthorized, "Session was invalid.") }
            }
          }
        }
      }
    } ~
    path("search") {
      entity(as[String]) { search =>
        post {      
          cookie(sessionCookieName) { c => r =>
            val user = User(UUID.fromString(c.content))
            user match {
              case Some(u) =>
                complete {
                  var users = u.search(search)
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
                  setCookie(HttpCookie(sessionCookieName, s.toString())) {
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
        cookie(sessionCookieName) { c => r =>
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
    path("admin") {
      get {
        cookie(sessionCookieName) { c => r =>
          val user = User(UUID.fromString(c.content))
          user match {
            case Some(u) =>
              if(u.admin)
                complete {
                  ""
                }
              else
                HttpResponse(spray.http.StatusCodes.Unauthorized, "Not an admin.")
            case None =>
              HttpResponse(spray.http.StatusCodes.Unauthorized, "Session was invalid.")
          }
        }
      }
    }
    path("user" / IntNumber) { userId => 
      delete {
        cookie(sessionCookieName) { c => r =>
          val user = User(UUID.fromString(c.content))
          user match {
            case Some(u) =>
              if(u.admin) {
                val deleteUser = User(userId)
                deleteUser match {
                  case Some(du) =>
                    complete {
                      du.delete()
                      ""
                    }
                  case None =>
                    HttpResponse(spray.http.StatusCodes.NotFound, "User not found.")
                }
              }
              else {
                HttpResponse(spray.http.StatusCodes.Unauthorized, "Not an admin.")
              }
            case None =>
              HttpResponse(spray.http.StatusCodes.Unauthorized, "Session was invalid.")
          }
        }
      }
    }
    path("user" / "promote" / IntNumber) { userId => 
      post {
        cookie(sessionCookieName) { c => r =>
          val user = User(UUID.fromString(c.content))
          user match {
            case Some(u) =>
              if(u.admin) {
                val promoteUser = User(userId)
                promoteUser match {
                  case Some(du) =>
                    complete {
                      ""
                    }
                  case None =>
                    HttpResponse(spray.http.StatusCodes.NotFound, "User not found.")
                }
              }
              else {
                HttpResponse(spray.http.StatusCodes.Unauthorized, "Not an admin.")
              }
            case None =>
              HttpResponse(spray.http.StatusCodes.Unauthorized, "Session was invalid.")
          }
        }
      }
    }
  }
  /*

  val route = 
    path("requests") {
      get {
        cookie(sessionCookieName) { c => r =>
          val user = User(UUID.fromString(c.content))
          val formKey = UUID.randomUUID().toString()
          setCookie(HttpCookie(formkeyCookieName, formKey)) {
            user match {
              case Some(u) =>
                complete {
                  val requests = u.friendRequests
                  ""
                }
              case None =>
                // Reject
                complete { HttpResponse(spray.http.StatusCodes.Unauthorized, "Session was invalid.") }
            }
          }
        }
      } ~
      post {
        entity(as[RelationshipRequestMessage]) { message =>
          cookie(sessionCookieName) { c => r =>
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
          cookie(sessionCookieName) { c => r =>
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
        cookie(sessionCookieName) { c => r =>
          val user = User(UUID.fromString(c.content))
          val formKey = UUID.randomUUID().toString()
          setCookie(HttpCookie(formkeyCookieName, formKey)) {
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
                complete { HttpResponse(spray.http.StatusCodes.Unauthorized, "Session was invalid.") }
                
            }
          }
        }
      } ~
      post {
        cookie(sessionCookieName) { sessionC => _ =>
          cookie(formkeyCookieName) { sessionK => _ =>
            val user = User(UUID.fromString(sessionC.content))
            user match {
              case Some(u) =>
                formFields('formkey, 'profileName, 'profileAddress, 'profileCurrentPassword, 'profileNewPassword, 'profileNewPasswordConfirm) { 
                  (formkey, name, address, cPassword, nPassword, nPasswordConf) =>

                  if(formkey == sessionK.content) {
                    if(u.checkPassword(cPassword)) {

                      (validName(name), validAddress(Some(address)), (nPassword == nPasswordConf)) match {
                        case (true, true, true) =>
                          try {
                            if(!nPassword.isEmpty) {
                              (validPassword(nPassword), validPassword(nPasswordConf)) match {
                                case (true, true) =>
                                  u.password = nPassword
                                case(_, _) =>
                                  HttpResponse(spray.http.StatusCodes.BadRequest, "Not valid input.")
                              }
                            }

                            u.name = name
                            u.address = Some(address)
                            complete {
                              // Get new profile page
                              ""
                            }
                          
                          } catch {
                            case dbe: DbError       => complete { HttpResponse(spray.http.StatusCodes.InternalServerError, s"Database not accessible: $dbe.s") }
                            case ue:  UserException => complete { HttpResponse(spray.http.StatusCodes.Unauthorized, s"Not valid input: $ue.s") }
                          }

                        case (_,_,_) => 
                          complete { HttpResponse(spray.http.StatusCodes.BadRequest, "Not valid input.") }
                        }
                    } else {
                      complete { HttpResponse(spray.http.StatusCodes.Unauthorized, "Wrong password.") }
                    }
                  } else {
                    complete { HttpResponse(spray.http.StatusCodes.Unauthorized, "Form key violation.") }
                  }
              }
              case None =>
                complete { HttpResponse(spray.http.StatusCodes.Unauthorized, "Session was invalid.") }
            }
          }
        }
      }
    } ~
    path("friends") {
      get {
        cookie(sessionCookieName) { c => r =>
          val user = User(UUID.fromString(c.content))
          val formKey = UUID.randomUUID().toString()
          setCookie(HttpCookie(formkeyCookieName, formKey)) {
            user match {
              case Some(u) =>
                complete {
                  // Get 
                  ""
                }
              case None =>
                // Reject
                complete { HttpResponse(spray.http.StatusCodes.Unauthorized, "Session was invalid.") }
            }
          }
        }
      }
    } ~
    path("search") {
      entity(as[String]) { search =>
        post {      
          cookie(sessionCookieName) { c => r =>
            val user = User(UUID.fromString(c.content))
            user match {
              case Some(u) =>
                complete {
                  var users = u.search(search)
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
                  setCookie(HttpCookie(sessionCookieName, s.toString())) {
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
        cookie(sessionCookieName) { c => r =>
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
    path("admin") {
      get {
        cookie(sessionCookieName) { c => r =>
          val user = User(UUID.fromString(c.content))
          user match {
            case Some(u) =>
              if(u.admin)
                complete {
                  ""
                }
              else
                HttpResponse(spray.http.StatusCodes.Unauthorized, "Not an admin.")
            case None =>
              HttpResponse(spray.http.StatusCodes.Unauthorized, "Session was invalid.")
          }
        }
      }
    }
    path("user" / IntNumber) { userId => 
      delete {
        cookie(sessionCookieName) { c => r =>
          val user = User(UUID.fromString(c.content))
          user match {
            case Some(u) =>
              if(u.admin) {
                val deleteUser = User(userId)
                deleteUser match {
                  case Some(du) =>
                    complete {
                      du.delete()
                      ""
                    }
                  case None =>
                    HttpResponse(spray.http.StatusCodes.NotFound, "User not found.")
                }
              }
              else {
                HttpResponse(spray.http.StatusCodes.Unauthorized, "Not an admin.")
              }
            case None =>
              HttpResponse(spray.http.StatusCodes.Unauthorized, "Session was invalid.")
          }
        }
      }
    }
    path("user" / "promote" / IntNumber) { userId => 
      post {
        cookie(sessionCookieName) { c => r =>
          val user = User(UUID.fromString(c.content))
          user match {
            case Some(u) =>
              if(u.admin) {
                val promoteUser = User(userId)
                promoteUser match {
                  case Some(du) =>
                    complete {
                      ""
                    }
                  case None =>
                    HttpResponse(spray.http.StatusCodes.NotFound, "User not found.")
                }
              }
              else {
                HttpResponse(spray.http.StatusCodes.Unauthorized, "Not an admin.")
              }
            case None =>
              HttpResponse(spray.http.StatusCodes.Unauthorized, "Session was invalid.")
          }
        }
      }
    }*/
}
