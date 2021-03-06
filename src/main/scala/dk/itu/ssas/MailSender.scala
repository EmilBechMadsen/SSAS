package dk.itu.ssas

import akka.actor.{ Actor, ActorLogging, Props }
import java.util.UUID

sealed abstract class MailSenderMessage
case class ConfirmationMail(email: String, name: String, key: UUID)
extends MailSenderMessage

class MailSender extends Actor with ActorLogging {
  import org.apache.commons.mail.{ EmailException, HtmlEmail }
  import Settings.{ baseUrl, siteName }
  import Settings.email._

  def receive = {
    case ConfirmationMail(email, name, confirmationGuid) => confirmationMail(email, name, confirmationGuid)
  }

  def sendEmail(email: String, name: String, subject: String, body: String): Unit = {
    val message = new HtmlEmail()
    try {
      message.setHostName(host) // Ofc needs to have smtp running locally (default port 25)
      message.addTo(email, name)
      message.setFrom(address, siteName)
      message.setSubject(subject)
      message.setHtmlMsg(body)
      message.send()
      log.info(s"Email to $email sent")
    } catch {
      case e: EmailException => log.error(s"Error sending mail to $email")
    }
  }

  def confirmationMail(email: String, name: String, confirmationGuid: UUID): Unit = {
    log.info(s"Sending confirmation mail to $email")
    val url = s"$baseUrl/confirm/$confirmationGuid"
  
    val subject = "Thanks for your interest in Raptor Dating. Please confirm your registration."
    val body = s"Hi $name <br><br>" +
                "Please confirm your registration by navigating to this address:<br><br>" +
               s"$url <br><br>" +
                "Regards, The Raptor Dating Team" 

    // Send confirmation mail
    sendEmail(email, name, subject, body)
  }
}
