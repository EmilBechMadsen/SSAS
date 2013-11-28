package dk.itu.ssas.remotes

import dk.itu.ssas.model.RemoteUser
import scala.concurrent.Future

trait RemoteSite {
  type IdType <: Any 

  val name: String
  def search(s: String): Future[List[RemoteUser]]
  def get(id: IdType): Future[RemoteUser]
  def all: Future[List[RemoteUser]]
}
