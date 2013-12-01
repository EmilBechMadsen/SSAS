package dk.itu.ssas.services

import dk.itu.ssas.model.UserExceptions

trait AdminService extends SsasService with UserExceptions {
  import dk.itu.ssas.model._
  import dk.itu.ssas.page._
  import dk.itu.ssas.page.request._
  import dk.itu.ssas.Settings.baseUrl
  import dk.itu.ssas.Validate._
  import java.util.UUID
  import spray.http._
  import spray.routing._
  import spray.routing.HttpService._

  val adminRoute = {
    pathPrefix("admin") {
      pathEnd {
        adminPage()
      } ~
      path("createUser") {
        post {
          createUser()
        }
      }~
      path("toggleAdmin" / IntNumber) { userId =>
        post {
          toggleAdmin(userId)
        }
      } ~
      path("delete" / IntNumber) { userId =>
        post {
          deleteUser(userId)
        }
      }~
      path("revoke" / JavaUUID) { key =>
        post {
          revokeApiKey(key)
        }
      }~
      path("createAPIKey") {
        post {
          createApiKey()
        }
      }
    }
  }

  private def adminPage(): ReqCon = withAdmin { implicit su =>
    get {
      html { implicit formKey =>
        complete {
          AdminPage("Admin Area", NoRequest())
        }
      }
    }
  }

  private def createUser(): ReqCon = withAdmin { implicit su =>
   withFormKey {
      formFields('signupEmail, 'signupName, 'signupPassword, 'signupPasswordConfirm) {
        (email, name, pass1, pass2) =>
        if (pass1 == pass2 && 
            validEmail(email) && 
            validName(name) && 
            validPassword(pass1)) {
          try {
            User.create(name, None, email, pass1, true) match {
              case Some(u) =>
                redirect(s"$baseUrl/admin", StatusCodes.SeeOther)
              case None    => complete {
                HttpResponse(StatusCodes.InternalServerError)
              }
            } 
          } catch {
            case eee: ExistingEmailException =>
              error(StatusCodes.BadRequest, "That email is already used.")
          }
        } else error(StatusCodes.BadRequest, "Invalid information.")
      }
    }
  }

  private def toggleAdmin(userId: Int): ReqCon = withAdmin { implicit su =>
    withFormKey {
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
              HttpResponse(StatusCodes.BadRequest, "That user does not exist.")
            }
          }
        }
      } catch {
        case dbe: DbError => complete {
          HttpResponse(StatusCodes.InternalServerError, "Database error.")
        }
        case ue: UserException => complete {
          HttpResponse(StatusCodes.BadRequest, "Invalid info.")
        }
      }
    }
  }

  private def deleteUser(userId: Int): ReqCon = withAdmin { implicit su =>
    withFormKey {
      User(userId) match {
        case Some(deleteUser) => {
          deleteUser.delete()
          log.warn(s"User $userId deleted")
          redirect(s"${baseUrl}/admin", StatusCodes.SeeOther)
        }
        case None => complete { 
          HttpResponse(StatusCodes.NotFound, "User not found.")
        }
      }
    }
  }

  private def revokeApiKey(key: UUID): ReqCon = withAdmin { implicit su =>
    withFormKey {
      ApiKey(key) match {
        case Some(apiKey) => {
          apiKey.revoked = true
          log.info(s"API key $key revoked")
          redirect(s"${baseUrl}/admin", StatusCodes.SeeOther)
        }
        case None => complete {
          log.warn(s"API key $key could not be found to be revoked")
          HttpResponse(StatusCodes.NotFound, "API key not found")
        }
      }
    }
  }

  private def createApiKey(): ReqCon = withAdmin { implicit su =>
    withFormKey {
      ApiKey.create()
      redirect(s"${baseUrl}/admin", StatusCodes.SeeOther)
    }
  }
}
