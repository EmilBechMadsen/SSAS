package dk.itu.ssas.page

import dk.itu.ssas.page.request._
import dk.itu.ssas.model._

trait WebPage {
  type RequestType <: Request

  def header(title: String, user: Option[User]): HTML

  def content(request: RequestType): HTML

  def footer: HTML

  def render(request: RequestType): HTML = "" //header + content(request) + footer
}