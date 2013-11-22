package dk.itu.ssas.model

import dk.itu.ssas.db.DbAccess
import java.util.UUID
import scala.language.postfixOps
import scala.slick.driver.MySQLDriver.simple._
import scala.slick.driver.MySQLDriver.simple.Database.threadLocalSession

object ApiKey extends DbAccess {
  /** Retrieves an API key from the database.
    *
    * @param key - The key of the desired API key.
    * @return An API key, if it exists.
    */
  def apply(key: UUID): Option[ApiKey] = Db withSession {
    (for (a <- ApiKeys if a.key === key.toString()) yield a) firstOption
  }

  /** Creates a new API key in the database.
    *
    * @return The new API key.
    */
  def create(): ApiKey = Db withSession {
    val apiKey = ApiKey(UUID.randomUUID().toString(), false)
    ApiKeys insert apiKey

    apiKey
  }
}

case class ApiKey(private val _key: String, private var _revoked: Boolean) extends DbAccess {
  val key = UUID.fromString(_key)

  /** The status of the API key.
    *
    * @return True if the API key has been revoked, false otherwise.
    */
  def revoked: Boolean = _revoked

  /** Sets the status of the API key.
    *
    * @param revoke - The status to set.
    */
  def revoked_=(revoke: Boolean): Unit = Db withSession {
    (for (a <- ApiKeys if a.key === _key) yield a.revoked) update revoke
    _revoked = revoke
  }
}
