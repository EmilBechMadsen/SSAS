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
      path("") {
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
        } ~
        path("delete" / IntNumber) { userId =>
          delete {
            withSession { s =>
              withUser(s) { u =>
                withAdmin(u) {
                  withFormKey(s) {
                    User(userId) match {
                      case Some(deleteUser) => {
                        deleteUser.delete()
                        log.warn(s"User $userId deleted")
                        redirect(s"$baseUrl/admin", StatusCodes.SeeOther)
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
}
