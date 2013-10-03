package dk.itu.ssas.test

import dk.itu.ssas.db.DbAccess
import org.scalatest.FunSuite
import scala.language.postfixOps
import scala.slick.driver.MySQLDriver.simple._
import scala.slick.driver.MySQLDriver.simple.Database.threadLocalSession

class UserTest extends FunSuite with DatabaseTests with DbAccess {
  import dk.itu.ssas.model._

  var user1Id = 1
  val name  = "John Doe"
  val email = "john@doe.com"
  val pass  = "password1"
  var user2Id = 2

  test("Creating user") {
    val addr  = Some("Road 123\n521614 Place town, Place\nSweden")
    val user  = User.create(name, addr, email, pass)

    user match {
      case Some(u) => {
        assert(u.name     === name)
        assert(u.email    === email)
        assert(u.address  === addr)
        user1Id = u.id
      }
      case None    => assert(false)
    }
  }

  test("Creating second user") {
    val name  = "Jimbo Pantson"
    val email = "jim_bim@party.town.org"
    val pass  = "ilike2party"
    val addr  = Some("The Pad")
    val user  = User.create(name, addr, email, pass)

    user match {
      case Some(u) => {
        assert(u.name     === name)
        assert(u.email    === email)
        assert(u.address  === addr)
        user2Id = u.id
      }
      case None    => assert(false)
    }
  }

  test("Newly created user is unconfirmed") {
    val user = User(user1Id)

    user match {
      case Some(u) => {
        assert(u.isConfirmed === false)
      }
      case None    => assert(false)
    }
  }

  test("User is confirmed") {
    val user = User(user1Id)

    val key = Db withSession {
      (for {
        e <- EmailConfirmations if e.userId === user1Id
      } yield e) firstOption
    }

    (user, key) match {
      case (Some(u), Some(k)) => {
        assert(u.isConfirmed === false)
        u.confirm(k.key)
        assert(u.isConfirmed === true)
      }
      case _    => assert(false)
    }
  }

  test ("Set user to admin") {
    User(user1Id) match {
      case (Some(user)) => {
        user.admin = true
        assert(user.admin === true)
      }
      case None => assert(false)
    }
  }

  test("Search Works") {
    val search1 = User search "John"
    assert(search1.length === 1)
    assert(search1(0).id === user1Id)

    val search2 = User search "Pantson"
    assert(search2.length === 1)
    assert(search2(0).id === user2Id)
  }

  test("Request friendship") {
    val user1 = User(user1Id)
    val user2 = User(user2Id)

    (user1, user2) match {
      case (Some(u1), Some(u2)) => {
        u1.requestFriendship(u2, Friendship)
      }
      case _ => assert(false)
    }
  }

  test("Receive friendship request") {
    val user1 = User(user1Id)
    val user2 = User(user2Id)

    (user1, user2) match {
      case (Some(u1), Some(u2)) => {
        assert(u2.friendRequests(u1) === Friendship)
      }
      case _ => assert(false)
    }
  }

  test("Accept friendship request") {
    val user1 = User(user1Id)
    val user2 = User(user2Id)

    (user1, user2) match {
      case (Some(u1), Some(u2)) => {
        assert(u2.acceptFriendRequest(u1, Friendship) === true)
        assert(u2.friends(u1) === Friendship)
        assert(u1.friends(u2) === Friendship)
        assert(u2.friendRequests.isEmpty)
      }
      case _ => assert(false)
    }
  }

  test("Remove friend") {
    val user1 = User(user1Id)
    val user2 = User(user2Id)

    (user1, user2) match {
      case (Some(u1), Some(u2)) => {
        u1.removeFriend(u2)
        assert(u1.friends.isEmpty)
        assert(u2.friends.isEmpty)
      }
      case _ => assert(false)
    }
  }

  test("Reject friendship") {
    val user1 = User(user1Id)
    val user2 = User(user2Id)

    (user1, user2) match {
      case (Some(u1), Some(u2)) => {
        u2.requestFriendship(u1, Bromance)
        assert(u1.friendRequests(u2) === Bromance)
        u1.rejectFriendRequest(u2, Bromance)
        assert(u1.friendRequests.isEmpty)
        assert(u1.friends.isEmpty)
      }
      case _ => assert(false)
    }
  }

  test("User can log in") {
    val user = User.login(email, pass)

    user match {
      case Some(u) => {
        assert(u.session isDefined)
      }
      case None    => assert(false)
    }
  }

  test("User can log out") {
    val user = User(user1Id)

    user match {
      case Some(u) => {
        assert(u.session.isDefined)
        u.logout()
        assert(u.session === None)
      }
      case None    => assert(false)
    }
  }

  test("Change name") {
    val name = "John Doe"
    val user = User(user1Id)

    user match {
      case Some(u) => {
        u.name = name
        assert(u.name === name)
      }
      case None    => assert(false)
    }

    val changedUser = User(1)
    changedUser match {
      case Some(u) => assert(u.name === name)
      case None    => assert(false)
    }
  }

  test("Change email") {
    val email = "jane@doe.net"
    val user  = User(user1Id)

    user match {
      case Some(u) => {
        u.email = email
        assert(u.email === email)
      }
      case None    => assert(false)
    }

    val changedUser = User(1)
    changedUser match {
      case Some(u) => assert(u.email === email)
      case None    => assert(false)
    }
  }

  test("Delete user") {
    val user2 = User(user2Id)

    user2 match {
      case Some(u) => {
        u.delete()
        assert(User(user2Id) === None)
      }
      case None => assert(false)
    }
  }
}
