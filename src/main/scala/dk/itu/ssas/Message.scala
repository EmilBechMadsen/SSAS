package dk.itu.ssas

import spray.json._	
import spray.httpx.SprayJsonSupport._
import DefaultJsonProtocol._ 
import java.util.UUID
import dk.itu.ssas.model._

abstract class SSASMessage
case class SignUpMessage(name: String, email: String, password: String) extends SSASMessage
case class ConfirmMailMessage(token: UUID, password: String) extends SSASMessage
case class RelationshipRequestMessage(userId: Int, rel: Relationship) extends SSASMessage
case class RelationshipConfirmationMessage(userId: Int, accepted: Boolean) extends SSASMessage
case class LogInMessage(email: String, password: String) extends SSASMessage

object SSASProtocol extends DefaultJsonProtocol {
	// Stolen from YACS
	implicit object UuidFormat extends RootJsonFormat[UUID] {
	  def write(x: UUID) = JsString(x.toString())
    def read(value: JsValue) = value match {
      case JsString(x) => UUID.fromString(x)
      case x => 
        deserializationError("Expected UUID as JsString, but got $x")
    }
  }

  implicit object RelationshipFormat extends RootJsonFormat[Relationship] {
    def write(a: Relationship) = {
      a match {
        case Friendship => JsString("friendship")
        case Romance    => JsString("romance")
        case Bromance   => JsString("bromance")
        case x          => deserializationError("Not a Relationship!")
      }
    }

    def read(value: JsValue) = {
      value match {
        case JsString("FRIENDSHIP") => Friendship
        case JsString("ROMANCE")    => Romance
        case JsString("BROMANCE")   => Bromance
        case x                      => deserializationError("Not a Relationship!")
      }
    }
  }
}

object SSASMessageProtocol extends DefaultJsonProtocol {
	import dk.itu.ssas.SSASProtocol._

  implicit val SignUpMessageFormat = 
    jsonFormat3(SignUpMessage)

  implicit val ConfirmMailMessageFormat =
  	jsonFormat2(ConfirmMailMessage)

  implicit val RelationshipRequestMessageFormat =
    jsonFormat2(RelationshipRequestMessage)

  implicit val RelationshipConfirmationMessageFormat =
    jsonFormat2(RelationshipConfirmationMessage)

  implicit val LogInMessageFormat =
    jsonFormat2(LogInMessage)
}

