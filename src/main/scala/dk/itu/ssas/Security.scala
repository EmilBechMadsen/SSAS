package dk.itu.ssas

object Security {
  import org.mindrot.jbcrypt.BCrypt
  import java.security.SecureRandom
  import java.math.BigInteger

  private val random = new SecureRandom()

  def newPassword(password: String): (String, String) = {
    val salt = new BigInteger(130, random).toString(16)

    // FIXME: Is this the right way to do it?
    (BCrypt.hashpw(password, BCrypt.gensalt(10, random)), salt)
  }

  def checkPassword(password: String, hash: String, salt: String): Boolean = {
    BCrypt.checkpw(password, hash)
  }
}
