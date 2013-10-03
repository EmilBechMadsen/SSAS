package dk.itu.ssas.page

import dk.itu.ssas.page.request._
import dk.itu.ssas.model._

trait WebPage {
  type RequestType <: Request

  protected def header(title: String, key: Key, user: Option[User]): HTML

  protected def content(request: RequestType, key: Key): HTML

  protected def footer: HTML

  def render(title: String, key: Key, user: Option[User], request: RequestType): HTML = {
  	val page = new StringBuilder()
  	val head = header(title, key, user)
  	val body = content(request, key)
  	page.append(head).append(body).append(footer).toString()
  }
}