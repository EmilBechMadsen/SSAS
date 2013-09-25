package dk.itu.ssas.page

import dk.itu.ssas.model._

class EditProfilePage(user: User) extends LoggedInPage(user) {
  def receive = {
    case _ =>
  }

  def asHTML: HTML = ""
}