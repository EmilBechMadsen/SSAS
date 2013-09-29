package dk.itu.ssas.test

import dk.itu.ssas.db.DbAccess
import org.scalatest.{BeforeAndAfterAll, FunSuite}

trait DatabaseTests extends FunSuite with BeforeAndAfterAll with DbAccess {
  import scala.language.postfixOps
  import scala.slick.driver.MySQLDriver.simple._
  import scala.slick.driver.MySQLDriver.simple.Database.threadLocalSession
  import scala.slick.jdbc.{GetResult, StaticQuery => Q}

  val dropStmts = List(
    "DROP TABLE IF EXISTS ssas.user_hobby",
    "DROP TABLE IF EXISTS ssas.hobby;",
    "DROP TABLE IF EXISTS ssas.email_confirmation;",
    "DROP TABLE IF EXISTS ssas.friend_request;",
    "DROP TABLE IF EXISTS ssas.session;",
    "DROP TABLE IF EXISTS ssas.admin;",
    "DROP TABLE IF EXISTS ssas.friend;",
    "DROP TABLE IF EXISTS ssas.user;")

  def resetDb = Db withSession {
    dropStmts foreach (s => (Q.u + s).execute)
    ddl create
  }

  override def beforeAll = Db withSession {
    ddl create
  }

  override def afterAll = Db withSession {
    dropStmts foreach (s => (Q.u + s).execute)
  }
}
