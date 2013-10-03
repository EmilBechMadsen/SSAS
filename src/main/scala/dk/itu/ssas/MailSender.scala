package dk.itu.ssas

import akka.actor.{ Actor, ActorLogging, Props }
import java.util.UUID

sealed abstract class MailSenderMessage extends SSASMessage
case class ConfirmationMail(email: String, name: String, key: UUID) extends SSASMessage

class MailSender extends Actor with ActorLogging {
  import org.apache.commons.mail.HtmlEmail

  val BaseUrl = "http://localhost:8080"

  def receive =  {
    case ConfirmationMail(email, name, confirmationGuid) => confirmationMail(email, name, confirmationGuid)
  }

  def sendEmail(email: String, name: String, subject: String, body: String) = {
    val message = new HtmlEmail()
    message.setHostName("localhost") // Ofc needs to have smtp running locally (default port 25)
    message.addTo(email, name)
    message.setFrom("noreply@raptordating.com", "Raptor Dating")
    message.setSubject(subject)
    message.setHtmlMsg(body)
    message.send()
  }

  def confirmationMail(email: String, name: String, confirmationGuid: UUID) = {
    log.info(s"Sending confirmation mail to $email")
    val url = s"$BaseUrl/confirm/$confirmationGuid"
  
    val subject = "Thanks for your interest in Raptor Dating. Please confirm your registration."
    val body = s"Hi $name <br><br>" +
                "Please confirm your registration by navigating to this address:<br><br>" +
               s"$url <br><br>" +
               "Regards, The Raptor Dating Team" 

    // Send confirmation mail
    sendEmail(email, name, subject, body)
  }
}
