package dk.itu.ssas.page

import dk.itu.ssas.page.request._

object FriendsPage extends LoggedInPage {
  type RequestType = FriendsPageRequest

  def content(request: FriendsPageRequest): HTML = ""
}