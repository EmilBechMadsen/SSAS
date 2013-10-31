package dk.itu.ssas

object Validate {
  import org.apache.commons.validator.routines.EmailValidator

  private val ev = EmailValidator.getInstance()

  /** Checks an email address for validity
    *
    * @param e - The email address to check
    * @return Returns true if valid, false otherwise
    */
  def validEmail(e: String): Boolean = ev.isValid(e)

  /** Checks a user name for validity
    *
    * @param n - The user name to check
    * @return Returns true if valid, false otherwise
    */
  def validName(n: String): Boolean = {
    import Settings.security._

    n.length >= minName && n.length <= maxName && n.matches(nameWhitelist)
  }

  /** Checks a password for validity
    *
    * @param p - The password to check
    * @return Returns true if valid, false otherwise
    */
  def validPassword(p: String): Boolean = {
    import Settings.security._

    p.length >= minPassword && p.length <= maxPassword
  }

  /** Checks a hobby for validity
    *
    * @param h - The hobby to check
    * @return Returns true if valid, false otherwise
    */
  def validHobby(h: String): Boolean = {
    import Settings.security._

    h.length >= minHobby && h.length <= maxHobby && h.matches(hobbyWhitelist)
  }

  /** Checks an address for validity
    *
    * @param a - The address to check
    * @return Returns true if valid, false otherwise
    */
  def validAddress(a: Option[String]): Boolean = {
    import Settings.security._

    a match {
      case Some(a) =>
        a.length >= minAddr && a.length <= maxAddr && a.matches(addrWhitelist)
      case None =>
        true
    }
  }
}
