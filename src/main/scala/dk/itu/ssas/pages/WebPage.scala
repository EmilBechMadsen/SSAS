package dk.itu.ssas.page

import dk.itu.ssas.page.request._

trait WebPage {
  type RequestType <: Request

  def header: HTML

  def content(request: RequestType): HTML

  def footer: HTML

  def render(request: RequestType): HTML = header + content(request) + footer
}