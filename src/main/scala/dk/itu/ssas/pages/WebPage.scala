package dk.itu.ssas.page

import dk.itu.ssas.page.request._
import dk.itu.ssas.model._

trait WebPage {
  type RequestType <: Request

  def header(title: String, key: Int, user: Option[User]): HTML

  def content(request: RequestType, key: Int): HTML

  def footer: HTML

  def render(title: String, key: Int, user: Option[User], request: RequestType): HTML = {
  	val page = new StringBuilder()
  	val head = header(title, key, user)
  	val body = content(request, key)
  	page.append(head).append(body).append(footer).toString()
  }
}