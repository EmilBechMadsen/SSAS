package dk.itu.ssas.page

import akka.actor.{Actor, ActorLogging}

abstract class Page extends Actor with ActorLogging with HTMLElement {
  def topBar: TopBar
}