package dk.itu.ssas.model

import dk.itu.ssas.db.DbAccess
import java.sql.Timestamp
import java.util.UUID
import scala.language.postfixOps
import scala.slick.driver.MySQLDriver.simple._
import scala.slick.driver.MySQLDriver.simple.Database.threadLocalSession

object Session extends DbAccess {
  def apply(): Session = Db withSession {
    import dk.itu.ssas.Settings.security.sessionTimeout

    val time     = System.currentTimeMillis()
    val creation = new Timestamp(time)
    val s = Session(UUID.randomUUID().toString(), None, creation)
    Sessions insert s

    s
  }

  def apply(key: UUID): Option[Session] = Db withSession {
    (for (s <- Sessions if s.key === key.toString()) yield s) firstOption
  }
}

case class Session(
    private val _key: String, 
    private val _userId: Option[Int],
    val expiration: Timestamp)
  extends DbAccess {
  import dk.itu.ssas.model.User
  import dk.itu.ssas.Settings.security.formKeyTimeout

  val key    = UUID.fromString(_key)
  val userId = _userId
  
  def user: Option[User] = _userId match {
    case Some(id) => User(id)
    case None     => None
  }

  /** Returns a new form key if the user is logged in
    *
    * @return The form key as a UUID
    */
  def newFormKey(): UUID = Db withSession {
    import java.sql.Timestamp

    val formKey  = UUID.randomUUID()
    val now      = System.currentTimeMillis()
    val creation = new Timestamp(now)

    FormKeys insert FormKey(formKey.toString(), _key, creation)

    formKey
  }

  /** Checks if a form key is valid, if it is, it removes it from the database
    *
    * @param key - The form key to check
    * @return True if the form key is valid, false otherwise
    */
  def checkFormKey(formKey: UUID): Boolean = Db withSession {
    import java.sql.Timestamp

    val now = System.currentTimeMillis()
    val exp = new Timestamp((now - formKeyTimeout * 60000))

    val fk = for {
      fk <- FormKeys if fk.key === formKey.toString() &&
                        fk.creation >= exp
    } yield fk

    fk firstOption match {
      case Some(f) => {
        fk delete;
        true
      }
      case None    => false
    }
  }
}
