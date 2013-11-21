package dk.itu.ssas.model

import dk.itu.ssas.db.DbAccess
import java.util.UUID
import scala.language.postfixOps
import scala.slick.driver.MySQLDriver.simple._
import scala.slick.driver.MySQLDriver.simple.Database.threadLocalSession

object ApiKey extends DbAccess {
  def apply(key: UUID): Option[ApiKey] = Db withSession {
    (for (a <- ApiKeys if a.key === key.toString()) yield a) firstOption
  }
}

case class ApiKey(private val _key: String, revoked: Boolean) {
  val key = UUID.fromString(_key)
}
