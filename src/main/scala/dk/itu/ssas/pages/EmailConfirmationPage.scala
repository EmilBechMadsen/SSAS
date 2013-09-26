package dk.itu.ssas.page

import dk.itu.ssas.page.request._

object EmailConfirmationPage extends LoggedOutPage {
  type RequestType = NoRequest
  
  def content(request: NoRequest): HTML = ""
}