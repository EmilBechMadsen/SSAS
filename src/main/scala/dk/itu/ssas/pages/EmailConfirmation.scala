package dk.itu.ssas.page

import akka.actor.{Actor, ActorLogging}

class EmailConfirmationPage extends Actor with LoggedOutPage with ActorLogging {
	def receive {
		case _ =>
	}
}