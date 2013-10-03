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
  import scala.language.postfixOps

  private def renewFormKey(c: String => RequestContext => Unit): RequestContext => Unit = {
    val formKey = UUID.randomUUID().toString()
    setCookie(HttpCookie(formkeyCookieName, formKey)) {
      c(formKey)
    }
  }

  private def postWithFormKey(c: RequestContext => Unit): RequestContext => Unit = {
    post {
      optionalCookie(formkeyCookieName) { ck =>
        ck match {
          case Some(cookieKey) => {
            formField('formkey) { formKey =>
              if (cookieKey.content == formKey) {
                c
              }
              else complete {
                HttpResponse(StatusCodes.Unauthorized, "XSRF protection kicked in. Please try again.")
              }
            }
          }
          case None => complete {
            HttpResponse(StatusCodes.Unauthorized, "XSRF protection kicked in. Please try again.")
          }
        }
      }
    }
  }

  private def withUser(c: User => RequestContext => Unit): RequestContext => Unit = {
    optionalCookie(sessionCookieName) { sc => 
      sc match {
        case Some(sessionCookie) => try {
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
        case None => redirect("/signup", StatusCodes.SeeOther)
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
              case Some(u) => complete {
                "Check your email for confirmation!"
              }
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
    path("login") {
      postWithFormKey {
        formFields('loginEmail, 'loginPassword) { (email, password) =>
          User.login(email, password) match {
            case Some(user) => user.session match {
              case Some(session) => {
                setCookie(HttpCookie(sessionCookieName, session.toString)) {
                  redirect(s"/profile/${user.id}", StatusCodes.SeeOther)
                }
              }
              case None => complete {
                HttpResponse(StatusCodes.InternalServerError)
              }
            }
            case None => redirect("/signup", StatusCodes.SeeOther)
          }
        }
      }
    } ~
    withUser { u =>
      userRoutes(u)
    } ~ 
    path("logout") {
      post {      
        cookie(sessionCookieName) { cookie =>
          User(UUID.fromString(cookie.content)) match {
            case Some(u) => {
              deleteCookie(cookie) {
                u.logout()
                redirect("/signup", StatusCodes.SeeOther)
              } 
            }
            case None => complete { HttpResponse(StatusCodes.InternalServerError) } 
          }
        }
      }
    } 

  def userRoutes(u: User): RequestContext => Unit = {
    path("requests") {
      get {
        html { formKey =>
          complete {
            ViewRequestsPage.render("Your friend requests", formKey, Some(u), ViewRequestsPageRequest(u))
          }
        }
      } ~
      postWithFormKey {
        formFields('friendRequestId, 'friendRequestKind, 'friendRequestAccept ?, 'friendRequestReject ?) {
          (friendId, requestKind, accept, reject) =>
          User(friendId.toInt) match {
            case Some(otherUser) =>
              try {
                (accept, reject) match {
                  case (Some(_), None) => {
                    val k = u.acceptFriendRequest(otherUser, Relationship(requestKind))
                  }
                  case (None, Some(_)) => {
                    val k = u.rejectFriendRequest(otherUser, Relationship(requestKind))
                  }
                  case (_,_) => {
                    complete { HttpResponse(StatusCodes.BadRequest, "Both or neither accept and reject was pushed.") }
                  }
                }
                redirect(s"/request", StatusCodes.SeeOther)
              } catch {
                case rde: RelationshipDeserializationException => complete { HttpResponse(StatusCodes.InternalServerError, "Invalid relationship.") }
              }
            case None =>
              complete { HttpResponse(StatusCodes.BadRequest, "User not found.") }
          }
        }
      }
    } ~
    path("profile" / IntNumber) { id =>
      get {
        if (u.id == id) {
          html { formKey =>
            complete {
              EditProfilePage.render("Profile: " + u.name, formKey, Some(u), EditProfilePageRequest(u))
            }
          }
        } else {
          val otherUser = User(UUID.fromString(id.toString()))
          otherUser match {
            case Some(other) =>
              html { formKey =>
                complete {
                  ProfilePage.render("Profile: " + other.name, formKey, Some(u), ProfilePageRequest(u, other))
                }
              }
            case None =>
              complete {
                HttpResponse(StatusCodes.NotFound, "The requested user does not exist.")
              }
          }
        }
      } ~
      postWithFormKey {
        path("edit") {
          path("info") {
            if (u.id == id) {
              formFields('profileName, 'profileAddress, 'profileCurrentPassword, 'profileNewPassword, 'profileNewPasswordConfirm) {
                (name, addr, currentPassword, newPassword, confirmPassword) =>
                  val nameChanged     = u.name != name
                  val addressChanged  = u.address != addr
                  val passwordChanged = newPassword != "" || confirmPassword != ""
                  try {
                    if (validPassword(currentPassword)) {
                      if (u.checkPassword(currentPassword)) {
                        if (nameChanged) {
                          if (validName(name)) {
                            u.name = name
                          } else {
                            complete {
                              HttpResponse(StatusCodes.BadRequest, "The new name is invalid.")
                            }
                          }
                        }
                        if (addr.isEmpty) {
                          u.address = None
                        } else {
                          if (addressChanged) {
                            if (validAddress(Some(addr))) {
                              u.address = Some(addr)
                            } else {
                              complete {
                                HttpResponse(StatusCodes.BadRequest, "The new address is invalid.")
                              }
                            }
                          }
                        }
                        if (passwordChanged) {
                          if (newPassword == confirmPassword && validPassword(newPassword)) {
                            u.password = newPassword
                          } else {
                            complete {
                              HttpResponse(StatusCodes.BadRequest, "The password could not be changed.")
                            }
                          }
                        }
                        redirect(s"/profile/${u.id}", StatusCodes.SeeOther)
                      } else {
                        complete {
                          HttpResponse(StatusCodes.Unauthorized, "You do not have permission to edit this profile.")
                        }
                      }
                    } else {
                      complete {
                        HttpResponse(StatusCodes.BadRequest, "Invalid password.")      
                      }
                    }
                  } catch {
                    case dbe: DbError => complete { HttpResponse(StatusCodes.InternalServerError, "Database error.") }
                    case ue: UserException => complete { HttpResponse(StatusCodes.BadRequest, "Invalid info.") }
                  }
              }
            } else {
              complete {
                HttpResponse(StatusCodes.Unauthorized, "You cannot edit another person's profile.")
              }
            }
          }~
          path("hobby") {
            path("add") {
              if (u.id == id) {
                formFields('profileNewHobby) { hobby =>
                  try {
                    if (validHobby(hobby)) {
                      u.addHobby(hobby)
                      redirect(s"/profile/${u.id}", StatusCodes.SeeOther)
                    } else {
                      complete {
                        HttpResponse(StatusCodes.BadRequest, "Invalid hobby.")
                      }
                    }
                  } catch {
                    case dbe: DbError => complete { HttpResponse(StatusCodes.InternalServerError, "Database error.") }
                    case ue: UserException => complete { HttpResponse(StatusCodes.BadRequest, "Invalid info.") }
                  }
                }
              } else {
                complete {
                  HttpResponse(StatusCodes.Unauthorized, "You cannot edit another person's profile.")
                }
              }
            }~
            path("remove") {
              if (u.id == id) {
                formFields('profileHobby) { hobby =>
                  try {
                    if (validHobby(hobby)) {
                      if (u.hobbies.exists { h => h == hobby }) {
                          u.removeHobby(hobby)
                          redirect(s"/profile/${u.id}", StatusCodes.SeeOther)
                      }
                    }
                    redirect(s"/profile/${u.id}", StatusCodes.SeeOther)
                  } catch {
                    case dbe: DbError => complete { HttpResponse(StatusCodes.InternalServerError, "Database error.") }
                    case ue: UserException => complete { HttpResponse(StatusCodes.BadRequest, "Invalid info.") }
                  }
                }
              } else {
                complete {
                  HttpResponse(StatusCodes.Unauthorized, "You cannot edit another person's profile.")
                }                
              }
            }
          }
        }~
        path("request") {
          if (u.id == id) {
            complete {
              HttpResponse(StatusCodes.BadRequest, "Sorry, you cannot have a relationship to yourself.")
            }
          } else {
            formFields('relationship) { relationship =>
              try {
                val otherUser = User(UUID.fromString(id.toString()))
                otherUser match {
                  case Some(other) =>
                    u.requestFriendship(other, Relationship(relationship))
                    redirect(s"/profile/${u.id}", StatusCodes.SeeOther)
                  case None =>
                    complete {
                      HttpResponse(StatusCodes.BadRequest, "User does not exist.")  
                    }
                }
              } catch {
                case e: RelationshipDeserializationException =>
                  complete {
                    HttpResponse(StatusCodes.InternalServerError, "Invalid relationship.")
                  }
              }
            }
          }
        }
      }
      }
    } ~
    path("friends") {
      get {
        html { formKey =>
          complete {
            FriendsPage.render("Your friends", formKey, Some(u), FriendsPageRequest(u))
          }
        }
      } ~
      postWithFormKey {
        formFields('friendRemoveId) { friendId =>
          User(friendId.toInt) match {
            case Some(otherUser) =>
                u.removeFriend(otherUser)
                redirect(s"/friends", StatusCodes.SeeOther)
            case None =>
              complete { HttpResponse(StatusCodes.BadRequest, "User not found.") }
          }
        }
      }
    } ~
    path("search") {
      entity(as[String]) { search =>
        post { 
          var users = u.search(search)
          html { formKey =>
            complete {
              SearchPage.render("Search results", formKey, Some(u), SearchPageRequest(users))
            }
          }
        }
      }
    } ~
    path("admin") {
      if (u.admin) {
        get {
          html { formKey =>
            complete {
              AdminPage.render("Admin Area", formKey, Some(u), AdminPageRequest(u))
            }
          }
        } ~
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
        } ~
        path("user" / IntNumber / "toggleAdmin") { userId => 
          postWithFormKey {
            try {
              User(UUID.fromString(userId.toString())) match {
                case Some(other) =>
                  if (other.admin) {
                    other.admin = false
                  } else {
                    other.admin = true
                  }
                  redirect(s"/admin", StatusCodes.SeeOther)
                case None =>
                  complete {
                    HttpResponse(spray.http.StatusCodes.BadRequest, "That user does not exist.")
                  }
              }
            } catch {
                case dbe: DbError => complete { HttpResponse(StatusCodes.InternalServerError, "Database error.") }
                case ue: UserException => complete { HttpResponse(StatusCodes.BadRequest, "Invalid info.") }
            }
          }
        }
      } else {
        complete {
          HttpResponse(spray.http.StatusCodes.Forbidden, "You must be an admin to enter this area.")
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
    }
}*/
