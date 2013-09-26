package dk.itu.ssas.page

import dk.itu.ssas.page.request._

object EditProfilePage extends LoggedInPage {
  type RequestType = EditProfilePageRequest

  def content(request: EditProfilePageRequest): HTML = ""
}