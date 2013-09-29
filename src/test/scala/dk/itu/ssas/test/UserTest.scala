package dk.itu.ssas.test

import dk.itu.ssas.db.DbAccess
import org.scalatest.FunSuite

class UserTest extends FunSuite with DatabaseTests {
  import dk.itu.ssas.model.User

  test("Creating user") {
    val name  = "John Doe"
    val email = "john@doe.com"
    val addr  = Some("Road 123\n521614 Place town, Place\nSweden")
    val pass  = "password1"
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

  test("Change name") {
    val name = "John Doe"
    val user = User(1)

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
    val user  = User(1)

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
}
