package dk.itu.ssas.model

protected trait UserExceptions {
  abstract sealed class UserException(s: String) extends Exception(s)

  case class InvalidAddressException(s: String = "Invalid address")
  extends UserException(s)
  
  case class InvalidEmailException(s: String = "Invalid email")
  extends UserException(s)

  case class InvalidHobbyException(s: String = "Invalid hobby")
  extends UserException(s)

  case class InvalidNameException(s: String = "Invalid name")
  extends UserException(s)

  case class InvalidPasswordException(s: String = "Invalid password")
  extends UserException(s)

  case class DbError(s: String = "There was a problem updating the database")
  extends UserException(s)
}
