package dk.itu.ssas.test

import dk.itu.ssas.db.DbAccess
import dk.itu.ssas.model._
import java.util.UUID
import org.scalatest.{ BeforeAndAfterEach, BeforeAndAfter, FunSuite }
import scala.language.postfixOps
import scala.slick.driver.MySQLDriver.simple._
import scala.slick.driver.MySQLDriver.simple.Database.threadLocalSession

class UserTest extends FunSuite 
                  with BeforeAndAfter
                  with UserExceptions 
                  with DatabaseTests 
                  with DbAccess
                  with TestData {

  var users: List[User] = _
  var confirmedUsers: List[User] = _

  before {
    users = randomUsers(2, true, false) map { p => p._1 } // with address, not confirmed
    confirmedUsers = randomUsers(2, true, true) map { p => p._1 }
  }

  test("Creating user") {
    val name  = "John Døe"
    val email = "john@doe.com"
    val pass  = "password1"
    val addr  = Some("Road 123\n521614 Place town, Place\nSweden")
    val user  = User.create(name, addr, email, pass)

    user match {
      case Some(u) => {
        assert(u.name     === name)
        assert(u.email    === email)
        assert(u.address  === addr)
      }
      case None    => assert(false)
    }
  }

  test("Newly created user is unconfirmed") {
    for (u <- users) assert(u.isConfirmed === false)
  }

  test("User is confirmed") {
    val user = users.head

    val key = Db withSession {
      (for {
        e <- EmailConfirmations if e.userId === user.id
      } yield e) firstOption
    }

    key match {
      case Some(k) => {
        assert(user.isConfirmed === false)
        user.confirm(UUID.fromString(k.key))
        assert(user.isConfirmed === true)
      }
      case _    => assert(false)
    }
  }

  test("Creating second, confirmed user") {
    val name  = "Jimbo Pantson"
    val email = "jim_bim@party.town.org"
    val pass  = "ilike2party"
    val addr  = Some("The Pad")
    val user  = User.create(name, addr, email, pass, true)

    user match {
      case Some(u) => {
        assert(u.name     === name)
        assert(u.email    === email)
        assert(u.address  === addr)

        val key = Db withSession {
          (for {
            e <- EmailConfirmations if e.userId === u.id
          } yield e) firstOption
        }

        key match {
          case None => assert(u.isConfirmed === true)
          case _    => assert(false)
        }
      }
      case None    => assert(false)
    }
  }

  test ("Set user to admin") {
    val user = confirmedUsers.head
    user.admin = true
    assert(user.admin === true)
  }

  test("Search Works") {
    val user1 = confirmedUsers.head
    val user2 = confirmedUsers.tail.head

    val search1 = user2 search (user1.name)
    assert(search1 exists { u => u.id == user1.id })

    val search2 = user1 search (user2.name)
    assert(search2 exists { u => u.id == user2.id })
  }

  test("You cannot hug a stranger") {
    val user1 = users.head
    val user2 = users.tail.head

    intercept[StrangerException] {
      user1 hug user2
    }
  }

  test("Request and accept friendship") {
    val user1 = confirmedUsers.head
    val user2 = confirmedUsers.tail.head

    user1.requestFriendship(user2, Friendship)
    // user2 has received friendship request
    assert(user2.friendRequests(user1) === Friendship)

    assert(user2.acceptFriendRequest(user1, Friendship) === true)
    assert(user2.friends(user1) === Friendship)
    assert(user1.friends(user2) === Friendship)
    assert(user2.isFriend(user1.id))
    assert(user1.isFriend(user2.id))
    assert(user2.friendRequests.isEmpty)
  }

  test("You can hug a friend, many times!") {
    val (u1, u2) = createFriends
    val unseen = u2.unseenHugs
    u1 hug u2
    assert(u2.unseenHugs === unseen+1)
    u1 hug u2
    assert(u2.unseenHugs === unseen+2)
  }

  test("You can mark a single hug as seen") {
    val (u1, u2) = createFriends
    u1 hug u2
    u1 hug u2
    val unseenBefore = u2.hugs._1
    u2 seenHug unseenBefore.head.id
    val unseenAfter = u2.hugs._1
    assert(unseenAfter.length === unseenBefore.length-1)
    assert(unseenAfter forall { h => unseenBefore exists { h1 => h == h1 } })
  }

  test("You can see unseen and seen hugs") {
    val (u1, u2) = createFriends
    u1 hug u2
    u1 hug u2

    val (unseenBefore, seenBefore) = u2 hugs

    u2 seenHug unseenBefore.head.id

    val (unseenAfter, seenAfter) = u2 hugs

    assert(unseenBefore.length === unseenAfter.length+1)
    assert(unseenAfter forall { h => unseenBefore exists { h1 => h == h1 } })
  }

  test("seenHugs marks all hugs as seen") {
    val (user1, user2) = createFriends

    val (unseenBefore, seenBefore) = user2.hugs

    user1 hug user2
    user1 hug user2

    val (unseenIntermediate, seenIntermediate) = user2.hugs

    assert(unseenIntermediate.length === unseenBefore.length+2)
    assert(unseenBefore forall { h => unseenIntermediate exists { h1 => h == h1 } })
    assert(seenBefore === seenIntermediate)

    user2.seenHugs

    val (unseenAfter, seenAfter) = user2.hugs

    assert(unseenAfter.isEmpty)
    assert(seenAfter.length === seenBefore.length+2)
    assert(unseenBefore forall { h => seenAfter exists { h1 => h == h1 } })
  }

  test("User can add hobbies") {
    val user = confirmedUsers.head

    val hobby = addRandomHobby(user)

    assert(user hasHobby hobby)
  }

  test("User can remove hobbies") {
    val user = confirmedUsers.head
    val hobby = addRandomHobby(user)

    assert(user hasHobby hobby)
    assert(user.hobbies exists { h => h == hobby })

    user removeHobby hobby

    assert((user hasHobby hobby) === false)
    assert(!(user.hobbies exists { h => h == hobby }))
  }

  test("Remove friend") {
    val (u1, u2) = createFriends
    u1.removeFriend(u2)
    assert(u1.friends.isEmpty)
    assert(u2.friends.isEmpty)
  }

  test("Reject friendship") {
    val user1 = confirmedUsers.head
    val user2 = confirmedUsers.tail.head

    user2.requestFriendship(user1, Bromance)
    assert(user1.friendRequests(user2) === Bromance)
    user1.rejectFriendRequest(user2, Bromance)
    assert(user1.friendRequests.isEmpty)
    assert(user1.friends.isEmpty)
  }

  test("User can log in") {
    val s    = Session()
    val (user, password) = randomUser(true, true) //User.create("Thomas", None, "thomas@thomas.dk", "hest1234", true)

    assert(user.session isEmpty, "User was already logged in")
    
    val newUser = User.login(user.email, password, s.key)
    newUser match {
      case Some(u) => assert(u.session isDefined, "User session was not defined")
                      assert(u.isLoggedIn === true)
      case None    => assert(false, "User could not login")
    }
    
  }

  test("User can log out") {
    val (user, password) = randomUser(true, true)
    val s = Session()

    val newUser = User.login(user.email, password, s.key)
    newUser match {
      case Some(u) => {
        assert(u.session isDefined)
        u.logout()
        assert(u.session === None)
        assert(u.isLoggedIn === false)
      }
      case None    => assert(false)
    }
  }

  test("User can't log in with wrong password") {
    val s    = Session()
    val user = confirmedUsers.head
    val wrongPassword = randomPassword

    assert(user.session isEmpty)
    User.login(user.email, wrongPassword, s.key)
    assert(user.session isEmpty)
  }

  test("Change name") {
    val user = confirmedUsers.head
    val newName = randomName

    user.name = newName
    assert(user.name === newName)

    val changedUser = User(user.id)
    changedUser match {
      case Some(u) => assert(u.name === newName)
      case None    => assert(false)
    }
  }

  test("Change email") {
    val user  = confirmedUsers.head
    val email = randomEmail

    user.email = email
    assert(user.email === email)

    val changedUser = User(user.id)
    changedUser match {
      case Some(u) => assert(u.email === email)
      case None    => assert(false)
    }
  }

  test("Change password") {
    val user = confirmedUsers.head
    val password = randomPassword

    user.password = password
    assert(user.checkPassword(password))

    val changedUser = User(user.id)
    changedUser match {
      case Some(u) => assert(u.checkPassword(password))
      case None    => assert(false)
    }
  }

  test("Delete user") {
    val user = confirmedUsers.head
    val id = user.id
    user.delete()
    assert(User(id) === None)
  }

  test("List all users") {
    val allUsers = User.all

    val f : User => Boolean = { u => allUsers exists { v => u == v }}
    assert(users forall f)
    assert(confirmedUsers forall f)
  }

  test("List all admins") {
    val user1 = confirmedUsers.head
    val user2 = confirmedUsers.tail.head

    val allAdminsBefore = User.allAdmins

    user1.admin = true
    assert(user1.admin)
    user2.admin = true
    assert(user2.admin)

    val allAdminsAfter = User.allAdmins
    assert(allAdminsAfter exists { u => u == user1 })
    assert(allAdminsAfter exists { u => u == user2 })
    assert(allAdminsAfter.length === allAdminsBefore.length+2)
  }
  
  test("Create ApiKey") {
    val n = ApiKey.list.length

    val key = ApiKey.create
    val keys = ApiKey.list

    assert(keys exists { k => k == key })

    val m = keys.length

    assert(m === n+1)
  }

  test("Revoke ApiKey") {
    val key = ApiKey.create
    val keys = ApiKey.list

    assert(keys exists { k => k == key })

    key.revoked = false

    assert(key.revoked === false)
  }

  test("Create and Validate Formkey") {
    val (user, password) = randomUser(true,true)
    val s = Session()
    val userLoggedIn = User.login(user.email, password, s.key)
    val sessionKey = user.session match {
      case Some(s) => s
      case None => throw StrangerException("Session not found")
    }

      
    userLoggedIn match {
      case Some(u) => u.session match {
        case Some(s) => {
          val session = Session(s) match {
            case Some(s) => s
            case None => throw StrangerException("No session for logged in user")
          }
          val formKey = session.newFormKey()
          assert(session.checkFormKey(formKey), "Formkey did not match what was expected.")
        }
        case None => assert(false, "No session found")
      }
      case None => assert(false, "No user found")
    }
  }
}
