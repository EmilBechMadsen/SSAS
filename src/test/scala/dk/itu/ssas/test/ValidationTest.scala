package dk.itu.ssas.test

import org.scalatest.FunSuite

class ValidationTest extends FunSuite {
  import dk.itu.ssas.model.User
  import dk.itu.ssas.Validate._

  test("Invalid email") {
    val invalidEmails = List(
      "",
      "; DROP TABLE",
      "@test.com",
      "test.com",
      "cake@power.fake")

    invalidEmails foreach (e => assertResult (false) { validEmail(e) })
  }

  test("Valid email") {
    val validEmails = List(
      "cnha@itu.dk",
      "test+plus@party.com",
      "w.w@www.tv")

    validEmails foreach (e => assertResult (true) { validEmail(e) })
  }

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

  test("Invalid hobby") {
    val invalidHobbies = List(
      "<HTML>",
      "; DROP TABLE USER;",
      "")

    invalidHobbies foreach (h => assertResult(false) { validHobby(h) })
  }

  test("Valid hobby") {
    val validHobbies = List(
      "HTML",
      "Horses and ponies",
      "C++")

    validHobbies foreach (h => assertResult(true) { validHobby(h) })
  }

  test("Invalid address") {
    val invalidAddresses = List (
      "<HTML>",
      "; DROP TABLE PARTY;",
      "")

    invalidAddresses foreach (a => assertResult(false) { validAddress(Some(a)) })
  }

  test("Valid address") {
    val validAddresses = List (
      "Sweeden",
      "Testvej 32, 1.th\n2300 København S\nDanmark",
      "Party town")

    validAddresses foreach (a => assertResult(true) { validAddress(Some(a)) })
  }
}
