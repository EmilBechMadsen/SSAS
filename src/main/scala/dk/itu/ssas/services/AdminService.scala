package dk.itu.ssas.services

import dk.itu.ssas.model.UserExceptions

object AdminService extends SsasService with UserExceptions {
  import dk.itu.ssas.model._
  import dk.itu.ssas.page._
  import dk.itu.ssas.page.request._
  import dk.itu.ssas.Settings.baseUrl
  import dk.itu.ssas.Validate._
  import spray.http._
  import spray.routing._
  import spray.routing.HttpService._

  def route = {
    pathPrefix("admin") {
      pathEnd {
        withSession { s =>
          withUser(s) { u =>
            withAdmin(u) {
              get {
                html(s) { (s, formKey) =>
                  complete {
                    AdminPage.render("Admin Area", formKey, Some(u), AdminPageRequest(u))
                  }
                }
              }
            }
          }
        }
      } ~
      path("createUser") {
        post {
          withSession { s =>
            withUser(s) { u =>
              withAdmin(u) {
                withFormKey(s) {
                  formFields('signupEmail, 'signupName, 'signupPassword, 'signupPasswordConfirm) {
                    (email, name, pass1, pass2) =>
                    if (pass1 == pass2 && validEmail(email) && validName(name) && validPassword(pass1)) {
                      try {
                        User.create(name, None, email, pass1, true) match {
                          case Some(u) =>
                            redirect(s"$baseUrl/admin", StatusCodes.SeeOther)
                          case None    => complete {
                            HttpResponse(StatusCodes.InternalServerError)
                          }
                        } 
                      } catch {
                        case eee: ExistingEmailException => complete { HttpResponse(StatusCodes.BadRequest, "User with that email already exists.") }
                      }
                    } else complete {
                      HttpResponse(StatusCodes.BadRequest, "Invalid information.")
                    }
                  }
                }
              }
            }
          }
        }
      }~
      path("toggleAdmin" / IntNumber) { userId =>
        post {
          withSession { s =>
            withUser(s) { u =>
              withAdmin(u) {
                withFormKey(s) {
                  try {
                    User(userId) match {
                      case Some(other) => {
                        if (other.admin) {
                          other.admin = false
                        } else {
                          other.admin = true
                        }

                        log.info(s"User $userId promoted")
                        redirect(s"$baseUrl/admin", StatusCodes.SeeOther)
                      }
                      case None => {
                        complete {
                          HttpResponse(spray.http.StatusCodes.BadRequest, "That user does not exist.")
                        }
                      }
                    }
                  } catch {
                    case dbe: DbError => complete { HttpResponse(StatusCodes.InternalServerError, "Database error.") }
                    case ue: UserException => complete { HttpResponse(StatusCodes.BadRequest, "Invalid info.") }
                  }
                }
              }
            }
          }
        }
      } ~
      path("delete" / IntNumber) { userId =>
        post {
          withSession { s =>
            withUser(s) { u =>
              withAdmin(u) {
                withFormKey(s) {
                  User(userId) match {
                    case Some(deleteUser) => {
                      deleteUser.delete()
                      log.warn(s"User $userId deleted")
                      redirect(s"${baseUrl}/admin", StatusCodes.SeeOther)
                    }
                    case None => complete { HttpResponse(StatusCodes.NotFound, "User not found.") }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
