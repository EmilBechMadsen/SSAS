package dk.itu.ssas.test

import org.scalatest.FunSuite

class ValidationTest extends FunSuite {
  import dk.itu.ssas.model.User
  import dk.itu.ssas.Validate._

  test("Invalid name") {
    val invalidNames = List(
      "",
      "<html>", 
      "; DROP TABLE USERS")
    
    invalidNames foreach (n => assert(validName(n) === false))
  }

  test("Valid name") {
    val validNames = List(
      "Sven", 
      "Christian Harrington", 
      "Sune Alkærsig", 
      "愛藍",
      "Адам")

    validNames foreach (n => assert(validName(n) === true))
  }

  test("Invalid password") {
    val invalidPasswords = List(
      "j",
      "pass",
      "432452")

    invalidPasswords foreach (p => assert(validPassword(p) === false))
  }

  test("Valid password") {
    val validPasswords = List(
      "dsadasf3a",
      "password", // FIXME: Should this be valid?
      "4324543242")

    validPasswords foreach (p => assert(validPassword(p) === true))
  }
}
