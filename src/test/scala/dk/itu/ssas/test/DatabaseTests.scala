package dk.itu.ssas.test

import dk.itu.ssas.db.DbAccess
import org.scalatest.{ BeforeAndAfterAll, FunSuite }

object DatabaseLock {
  var notRun = true

  def apply(): Boolean = {
    if (notRun) {
      notRun = false
      true
    } else {
      notRun
    }
  }
}

trait DatabaseTests extends FunSuite with BeforeAndAfterAll with DbAccess {
  import scala.language.postfixOps
  import scala.slick.driver.MySQLDriver.simple._
  import scala.slick.driver.MySQLDriver.simple.Database.threadLocalSession
  import scala.slick.jdbc.{ GetResult, StaticQuery => Q }

  val dropStmts = List(
    "DROP TABLE IF EXISTS hug;",
    "DROP TABLE IF EXISTS user_hobby;",
    "DROP TABLE IF EXISTS hobby;",
    "DROP TABLE IF EXISTS email_confirmation;",
    "DROP TABLE IF EXISTS friend_request;",
    "DROP TABLE IF EXISTS formkey;",
    "DROP TABLE IF EXISTS session;",
    "DROP TABLE IF EXISTS admin;",
    "DROP TABLE IF EXISTS friend;",
    "DROP TABLE IF EXISTS user;",
    "DROP TABLE IF EXISTS api_key;")

  def resetDb() = Db withSession {
    dropStmts foreach (s => (Q.u + s).execute)
    ddl create
  }

  override def beforeAll = Db withSession {
    DatabaseLock.synchronized {
      if (DatabaseLock()) {
        resetDb()
        dk.itu.ssas.Server.main(Array[String]())
      }
   } 
  }
}
