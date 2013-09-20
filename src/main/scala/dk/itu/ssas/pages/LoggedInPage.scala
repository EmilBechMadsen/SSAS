package dk.itu.ssas.page

import akka.actor.{Actor, ActorLogging}

trait LoggedInPage extends Page {
  override def topBar: TopBar = {
  	new TopBar("UNDERCONSTRUCTION.GIF")
  }
}