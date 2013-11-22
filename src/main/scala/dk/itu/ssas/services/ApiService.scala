package dk.itu.ssas.services

import dk.itu.ssas.model.UserExceptions

object ApiService extends SsasService with UserExceptions {
  import dk.itu.ssas.model._
  import dk.itu.ssas.Validate._
  import java.util.UUID
  import spray.http._
  import spray.httpx.SprayJsonSupport
  import spray.json._
  import spray.routing._
  import spray.routing.HttpService._

  object SSASJsonProtocol extends DefaultJsonProtocol {
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

  def route = {
    pathPrefix("api") {
      pathPrefix("user") {
        path("search") {
          post {
            withApiKey {
              entity(as[String]) { searchTerm =>
                val us = User.search(searchTerm).map(u => JsObject(("id", u.id.toJson), ("name", u.name.toJson)))

                complete {
                  us.toJson.toString()
                }
              }
            }
          }
        }~
        path("list") {
          get {
            withApiKey {
              complete {
                User.all.toJson.toString()
              }
            }
          }
        }~
        path(IntNumber) { id =>
          get {
            withApiKey {
              complete {
                User(id).toJson.toString()
              }
            }
          }
        }
      }
    }
  }
}
