package dk.itu.ssas.page.exception

sealed abstract class WebException extends Exception
case class NoUserException() extends WebException
case class UnexpectedUserException() extends WebException
case class NotAdminException() extends WebException
