package dk.itu.ssas.services

import dk.itu.ssas.model.UserExceptions

trait ApiService extends SsasService with UserExceptions {
  import dk.itu.ssas.model._
  import dk.itu.ssas.Validate._
  import java.util.UUID
  import spray.http._
  import spray.httpx.SprayJsonSupport._
  import spray.json._
  import spray.routing._
  import spray.routing.HttpService._

  private object SSASJsonProtocol extends DefaultJsonProtocol {
    implicit object UserFormat extends RootJsonFormat[User] {
      def write(u: User): JsValue = JsObject(
        ("id", u.id.toJson), 
        ("name", u.name.toJson),
        ("hobbies", u.hobbies.toJson)
      )

      def read(value: JsValue): User = value.asJsObject.getFields("id", "name", "hobbies") match {
        case Seq(JsNumber(id), JsString(name), JsArray(hobbies)) => User(id.toInt) match {
          case Some(user) => user
          case None       => deserializationError("No such user")
        }
        case _ => deserializationError("User expected")
      }
    }
  }

  import SSASJsonProtocol._

  private def userRoute(key: String) = pathPrefix("user") {
    path("search") {
      post {
        entity(as[String]) { searchTerm =>
          complete {
            log.info(s"""API: Search for "$searchTerm" from API key: $key""")
            User.search(searchTerm).map(u => JsObject(("id", u.id.toJson), ("name", u.name.toJson)))
          }
        }
      }
    }~
    path("list") {
      get {
        complete {
          log.info(s"API: Request for all users from API key: $key")
          User.all
        }
      }
    }~
    path(IntNumber) { id =>
      get {
        complete {
          log.info(s"API: Request for user $id from API key: $key")
          User(id)
        }
      }
    }
  }

  val apiRoute = {
    pathPrefix("api") {
      respondWithMediaType(MediaTypes.`application/json`) {
        headerValueByName("Authorization") { key =>
          withApiKey(key) {
            userRoute(key)
          }
        }
      }
    }
  }
}
