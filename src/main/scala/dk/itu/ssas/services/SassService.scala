package dk.itu.ssas.services

import spray.routing._

trait SsasService {
  import dk.itu.ssas.model._
  import dk.itu.ssas.Settings
  import java.util.UUID
  import org.apache.log4j.Logger
  import spray.http._
  import spray.routing.HttpService._

  val sessionCookieName = "ssas_session"

  val log = Logger.getLogger("Service")

  def setSessionCookie(session: UUID): Directive0 = {
    setCookie(HttpCookie(sessionCookieName, 
      session.toString(),
      path = Some("/"),
      httpOnly = true,
      secure = Settings.security.ssl))
  }

  protected def withSession(c: Session => RequestContext => Unit): RequestContext => Unit = {
    def runWithSession(c: Session => RequestContext => Unit): RequestContext => Unit = {
      val s = Session()
      setSessionCookie(s.key) {
        c(s)
      }
    }

    optionalCookie(sessionCookieName) { maybeCookie =>
      maybeCookie match {
        case Some(sessionCookie) => {
          try {
            Session(UUID.fromString(sessionCookie.content)) match {
              case Some(s) => c(s)
              case None    => {
                log.warn(s"Client had session cookie, but session was invalid (session: ${sessionCookie.content})")
                runWithSession(c)
              }
            }
          } catch {
            case e: IllegalArgumentException => {
              log.error("Could not deserialize session key, issuing a new session")
              runWithSession(c)
            }
          }
        }
        case None => runWithSession(c)
      }
    }
  }

  protected def newFormKey(s: Session)(c: String => RequestContext => Unit): RequestContext => Unit = {
    val formKey = s.newFormKey()
    c(formKey.toString())
  }

  protected def withFormKey(s: Session)(c: RequestContext => Unit): RequestContext => Unit = {
    formField('formkey) { formKey =>
      try {
        if (s.checkFormKey(UUID.fromString(formKey))) {
          c
        }
        else complete {
          log.warn(s"XSRF protection kicked in. Got $formKey for session ${s.key}")
          HttpResponse(StatusCodes.Unauthorized, "XSRF protection kicked in. Please try again.")
        }
      } catch {
        case e: IllegalArgumentException => complete {
          log.error("XSRF protection kicked in. Could not deserialize form key.")
          HttpResponse(StatusCodes.Unauthorized, "XSRF protection kicked in. Please try again.")
        }
      }
    }
  }

  protected def withUser(s: Session)(c: User => RequestContext => Unit): RequestContext => Unit = {
    s.user match {
      case Some(user) => c(user)
      case None => complete {
        log.warn(s"Unauthorized user trying to access user area")
        HttpResponse(StatusCodes.Unauthorized, "You need to be logged in to access this page")
      }
    }
  }

  protected def withAdmin(u: User)(c: RequestContext => Unit): RequestContext => Unit = {
    u.admin match {
      case true  => c
      case false => complete {
        log.warn(s"Non admin ${u.id} trying to access admin area")
        HttpResponse(spray.http.StatusCodes.Forbidden, "You must be an admin to enter this area.")
      }
    }
  }

  protected def html(s: Session)(c: (Session, String) => RequestContext => Unit): RequestContext => Unit = {
    respondWithMediaType(MediaTypes.`text/html`) {
      newFormKey(s) { key =>
        c(s, key)
      }
    }
  }
}
