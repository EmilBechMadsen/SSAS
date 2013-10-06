package dk.itu.ssas.services

import dk.itu.ssas.model.UserExceptions

object PublicService extends SsasService with UserExceptions {
  import dk.itu.ssas.model._
  import dk.itu.ssas.page._
  import dk.itu.ssas.page.request._
  import dk.itu.ssas.Validate._
  import java.util.UUID
  import spray.http._
  import spray.routing._
  import spray.routing.HttpService._

  def route = {
    path("signup") {
      get {
        withSession { s =>
          s.userId match {
            case Some(id) => redirect(s"/profile/$id", StatusCodes.SeeOther)
            case None     => html(s) { (s, formKey) =>
              complete {
                SignupPage.render("Sign up", formKey, None, NoRequest())
              }
            }
          }
        }
      } ~
      post {
        withSession { s =>
          withFormKey(s) {
            formFields('signupEmail, 'signupName, 'signupPassword, 'signupPasswordConfirm) {
              (email, name, pass1, pass2) =>
              if (pass1 == pass2 && validEmail(email) && validName(name) && validPassword(pass1)) {
                User.create(name, None, email, pass1) match {
                  case Some(u) => complete {
                    log.info(s"User ${u.id} created")
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
        }
      }
    } ~
    path("confirm" / JavaUUID) { token =>
      get {
        withSession { s =>
          User(token) match {
            case Some(user) => html(s) { (s, formKey) =>
              complete {
                EmailConfirmationPage.render("Confirm account", formKey, None, EmailConfirmationPageRequest(token))
              }
            }
            case None => complete {
              HttpResponse(StatusCodes.BadRequest, "Confirmation id was invalid.")
            }
          }
        }
      } ~
      post {
        withSession { s =>
          withFormKey(s) {
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
                        redirect(s"/profile/${user.id}", StatusCodes.SeeOther)
                      }
                      case None => redirect(s"/signup", StatusCodes.SeeOther)
                    }
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
        }
      }
    } ~
    path("login") {
      post {
        withSession { s =>
          withFormKey(s) {
            formFields('loginEmail, 'loginPassword) { (email, password) =>
              User.login(email, password, s.key) match {
                case Some(user) => 
                  redirect(s"/profile/${user.id}", StatusCodes.SeeOther)
                case None => 
                  redirect("/signup", StatusCodes.SeeOther)
              }
            }
          }
        }
      }
    } ~
    path("logout") {
      post {
          withSession { s =>
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
    }
  }
}