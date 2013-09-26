package dk.itu.ssas.page

import dk.itu.ssas.page.request._

object ViewRequestsPage extends LoggedInPage {
  type RequestType = ViewRequestsPageRequest

  def content(request: ViewRequestsPageRequest): HTML = ""
}