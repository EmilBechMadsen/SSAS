package dk.itu.ssas.page

import dk.itu.ssas.page.request._

object ProfilePage extends LoggedInPage {
  type RequestType = ProfilePageRequest  

  def content(request: ProfilePageRequest): HTML = ""
}