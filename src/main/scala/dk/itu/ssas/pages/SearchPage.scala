package dk.itu.ssas.page

import dk.itu.ssas.model._

class SearchPage(user: User) extends LoggedInPage(user) {
  def receive = {
    case _ =>
  }

  def asHTML: HTML = ""
}