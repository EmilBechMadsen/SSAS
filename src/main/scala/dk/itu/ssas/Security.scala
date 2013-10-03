package dk.itu.ssas

object Security {
  import org.mindrot.jbcrypt.BCrypt
  import java.security.SecureRandom
  import java.math.BigInteger

  private val random = new SecureRandom()

  /** Hashes a password
    *
    * @param password - The password to hash
    * @return The hashed password, along with the salt used
    */
  def newPassword(password: String): (String, String) = {
    val salt = new BigInteger(130, random).toString(16)

    // FIXME: Is this the right way to do it?
    (BCrypt.hashpw(password, BCrypt.gensalt(10, random)), salt)
  }

  /** Checks if a password is correct
    *
    * @param password - The cleartext password
    * @param hash - The hashed password
    * @param salt - The salt used
    * @return True if the password is correct
    */
  def checkPassword(password: String, hash: String, salt: String): Boolean = {
    BCrypt.checkpw(password, hash)
  }
}
