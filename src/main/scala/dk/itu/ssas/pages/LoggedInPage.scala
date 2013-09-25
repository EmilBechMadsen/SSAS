package dk.itu.ssas.page

import dk.itu.ssas.model._

abstract class LoggedInPage(val user: User) extends Page {
  
  def topBar: TopBar = {
    // Implemenetation of appropriate topbar here
    TopBar("UNDERCONSTRUCTION.GIF")
  }

}