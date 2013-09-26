package dk.itu.ssas.page

import dk.itu.ssas.page.request._

object SearchPage extends LoggedInPage {
  type RequestType = SearchPageRequest

  def content(request: SearchPageRequest): HTML = ""
}