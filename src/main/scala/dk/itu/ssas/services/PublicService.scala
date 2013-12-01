package dk.itu.ssas.services

import dk.itu.ssas.model.UserExceptions

trait PublicService extends SsasService with UserExceptions {
  import dk.itu.ssas.model._
  import dk.itu.ssas.page._
  import dk.itu.ssas.page.request._
  import dk.itu.ssas.Settings.baseUrl
  import dk.itu.ssas.Validate._
  import java.util.UUID
  import spray.http._
  import spray.routing._
  import spray.routing.HttpService._

  val publicRoute = {
    path("signup") {
      get {
        getSignup()
      } ~
      post {
        postSignup()
      }
    } ~
    path("confirm" / JavaUUID) { token =>
      get {
        getConfirm(token)
      } ~
      post {
        postConfirm(token)
      }
    } ~
    path("login") {
      post {
        login()
      }
    } ~
    path("logout") {
      post {
        logout()
      }
    }
  }

  private def getSignup(): ReqCon = withSession { implicit s =>
    s.userId match {
      case Some(id) => redirect(s"/profile/$id", StatusCodes.SeeOther)
      case None     => html { implicit formKey =>
        complete {
          SignupPage("Sign up", NoRequest())
        }
      }
    }
  }

  private def postSignup(): ReqCon = withSession { implicit s =>
    withFormKey {
      formFields('signupEmail, 'signupName, 'signupPassword, 'signupPasswordConfirm) {
        (email, name, pass1, pass2) =>
        if (pass1 == pass2 &&
            validEmail(email) &&
            validName(name) &&
            validPassword(pass1)) {
          try {
            User.create(name, None, email, pass1) match {
              case Some(u) => complete {
                log.info(s"User ${u.id} created")
                "Check your email for confirmation!"
              }
              case None    => error(StatusCodes.InternalServerError)
            }
          } catch {
            case e: UserException => error(StatusCodes.BadRequest, e.getMessage())
          }
        } else error(StatusCodes.BadRequest, "Invalid information.")
      }
    }
  }

  private def getConfirm(token: UUID): ReqCon = withSession { implicit s =>
    User(token) match {
      case Some(user) => html { implicit formKey =>
        complete {
          val req = EmailConfirmationPageRequest(token)
          EmailConfirmationPage("Confirm account", req)
        }
      }
      case None => error(StatusCodes.BadRequest, "Confirmation id was invalid.")
    }
  }

  private def postConfirm(token: UUID) = withSession { implicit s =>
    withFormKey {
      formFields('emailConfirmationPassword) { password =>
        User(token) match {
          case Some(user) => {
            if (user.checkPassword(password)) {
              user.confirm(token)
              log.info(s"User ${user.id} confirmed")
              User.login(user.email, password, s.key)

              user.session match {
                case Some(session) => setCookie(HttpCookie(sessionCookieName, 
                                   session.toString,
                                   path = Some("/"),
                                   httpOnly = true,
                                   secure = true)) {
                  redirect(s"$baseUrl/profile/${user.id}", StatusCodes.SeeOther)
                }
                case None => redirect(s"$baseUrl/signup", StatusCodes.SeeOther)
              }
            } else error(StatusCodes.Unauthorized, "Incorrect username or password.")
          }
          case None => error(StatusCodes.BadRequest, "Confirmation id was invalid.")
        }
      }
    }
  }

  private def login(): ReqCon = withSession { implicit s =>
    withFormKey {
      formFields('loginEmail, 'loginPassword) { (email, password) =>
        User.login(email, password, s.key) match {
          case Some(user) => user.session match {
            case Some(session) => setSessionCookie(session) {
              redirect(s"$baseUrl/profile/${user.id}", StatusCodes.SeeOther)
            }
            case None =>
              redirect(s"$baseUrl/signup", StatusCodes.SeeOther)
          }
          case None => 
            redirect(s"$baseUrl/signup", StatusCodes.SeeOther)
        }
      }
    }
  }

  private def logout(): ReqCon = withSession { implicit s =>
    cookie(sessionCookieName) { cookie =>
      User(UUID.fromString(cookie.content)) match {
        case Some(u) => {
          deleteCookie(cookie) {
            u.logout()
            redirect(s"/signup", StatusCodes.SeeOther)
          } 
        }
        case None => error(StatusCodes.InternalServerError)
      }
    }
  }
}
