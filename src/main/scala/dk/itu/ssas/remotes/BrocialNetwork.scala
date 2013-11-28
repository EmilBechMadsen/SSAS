package dk.itu.ssas.remotes

import akka.actor.ActorSystem

class BrocialNetwork(private val s: ActorSystem) extends RemoteSite {
  import akka.io.IO
  import akka.pattern.ask
  import akka.util.Timeout
  import dk.itu.ssas.model.RemoteUser
  import scala.concurrent.duration.Duration
  import scala.concurrent.Future
  import spray.can.client.HostConnectorSettings
  import spray.can.Http
  import spray.client.pipelining._
  import spray.http._
  import spray.httpx.SprayJsonSupport._
  import spray.json._

  type IdType = Int

  private implicit val system = s

  import system.dispatcher

  val baseUrl = "https://192.237.201.151/ssase13"
  val apiKey = "QWxhZGRpbjpTZXNhbSwgbHVrIGRpZyAwcCE="

  case class BrocialUser(url: String, hobbies: String, id: String, name: String)

  object BrocialUserJsonProtocol extends DefaultJsonProtocol {
    implicit val brocialUserFormat = jsonFormat4(BrocialUser)
  }
  import BrocialUserJsonProtocol._

  private val pipelineSingle: HttpRequest => Future[BrocialUser] = (
    addHeader("Authorization", s"basic $apiKey")
    ~> sendReceive
    ~> unmarshal[BrocialUser]
  )

  private val pipelineList: HttpRequest => Future[List[BrocialUser]] = (
    addHeader("Authorization", s"basic $apiKey")
    ~> sendReceive
    ~> unmarshal[List[BrocialUser]]
  )

  private def brocialUser2RemoteUser(b: BrocialUser): RemoteUser = {
    val url = s"$baseUrl/view/${b.id}/"
    RemoteUser(url, b.name)
  }

  val name = "The Brocial Network"

  def search(s: String): Future[List[RemoteUser]] = {
    val request = Get(s"$baseUrl/api/users/$s/?page=1")
    pipelineList(request).map(l => l.map(b => brocialUser2RemoteUser(b)))
  }

  def get(id: IdType): Future[RemoteUser] = {
    pipelineSingle(Get(s"$baseUrl/api/user/$id/")).map(b => brocialUser2RemoteUser(b))
  } 

  def all: Future[List[RemoteUser]] = {
    pipelineList(Get(s"""$baseUrl/api/users/""/?page=1""")).map(l => l.map(b => brocialUser2RemoteUser(b)))
  }
}
