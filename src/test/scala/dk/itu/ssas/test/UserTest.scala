package dk.itu.ssas.test

import org.scalatest.FunSuite

class UserTest extends FunSuite {
  import dk.itu.ssas.model.User

  test("Invalid name") {
    val invalidNames = List(
      "",
      "<html>", 
      "; DROP TABLE USERS")
    
    invalidNames foreach (n => assert(User.validName(n) === false))
  }

  test("Valid name") {
    val validNames = List(
      "Sven", 
      "Christian Harrington", 
      "Sune Alkærsig", 
      "愛藍",
      "Адам")

    validNames foreach (n => assert(User.validName(n) === true))
  }

  test("Invalid password") {
    val invalidPasswords = List(
      "j",
      "pass",
      "432452")

    invalidPasswords foreach (p => assert(User.validPassword(p) === false))
  }

  test("Valid password") {
    val validPasswords = List(
      "dsadasf3a",
      "password", // FIXME: Should this be valid?
      "4324543242")

    validPasswords foreach (p => assert(User.validPassword(p) === true))
  }
}
