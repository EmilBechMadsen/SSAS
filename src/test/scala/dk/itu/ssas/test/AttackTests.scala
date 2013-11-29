package dk.itu.ssas.test

import dk.itu.ssas.db.DbAccess
import dk.itu.ssas.model._
import java.util.UUID
import org.scalatest.{ BeforeAndAfterEach, BeforeAndAfter, FunSuite }
import scala.language.postfixOps
import scala.slick.driver.MySQLDriver.simple._
import scala.slick.driver.MySQLDriver.simple.Database.threadLocalSession

class AttackTest extends FunSuite
                    with BeforeAndAfter
                    with UserExceptions
                    with DatabaseTests
                    with DbAccess
                    with TestData {
  var confirmedUsers: List[User] = _

  before {
    confirmedUsers = randomUsers(1, true, true) map { p => p._1 }
  }

  test("XSS attack in hobbies") {
    val user = confirmedUsers.head
    intercept[InvalidHobbyException] {
      user.addHobby("""<script>alert("ALERT")</script>""")
    }
  }

  test("XSS attack in name") {
    intercept[InvalidNameException] {
      User.create("""<script>alert("ALERT")</script>""", None, randomEmail, randomPassword, true)
    }
  }
}
