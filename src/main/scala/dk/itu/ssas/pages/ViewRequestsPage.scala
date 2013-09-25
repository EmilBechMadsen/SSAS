package dk.itu.ssas.page

import dk.itu.ssas.model._

class ViewRequestsPage(user: User) extends LoggedInPage(user) {
  def receive = {
    case _ =>
  }

  def asHTML: HTML = ""
}