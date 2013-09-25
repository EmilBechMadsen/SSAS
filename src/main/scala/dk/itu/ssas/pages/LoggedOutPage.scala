package dk.itu.ssas.page

abstract class LoggedOutPage extends Page {

  def topBar: TopBar = {
    // Implemenetation of appropriate topbar here
    TopBar("UNDERCONSTRUCTION.GIF")
  }

}