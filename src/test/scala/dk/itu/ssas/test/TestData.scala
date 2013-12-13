package dk.itu.ssas.test

import dk.itu.ssas.model.UserExceptions
import dk.itu.ssas.db.DbAccess

trait TestData extends DbAccess with UserExceptions {
  import dk.itu.ssas.Settings
  import scala.util.Random
  import dk.itu.ssas.model._
  import dk.itu.ssas.Validate._

  def randomName: String = {
    val minName = Settings.security.minName
    val maxName = Settings.security.maxName
    val length = Random.nextInt(maxName-minName)+minName
    val result = Random.nextString(length)
    if (validName(result)) result else randomName
  }

  def randomPassword: String = {
    val minPassword = Settings.security.minPassword
    val maxPassword = Settings.security.maxPassword
    val length = Random.nextInt(maxPassword-minPassword)+minPassword
    val result = Random.nextString(length)
    if (validPassword(result)) result else randomPassword
  }

  def randomAddress: String = {
    val minAddr = Settings.security.minAddr
    val maxAddr = Settings.security.maxAddr
    val length = Random.nextInt(maxAddr-minAddr)+minAddr
    val result = Random.nextString(length)
    if (validAddress(Some(result))) result else randomAddress
  }

  def randomHobby: String = {
    val minHobby = Settings.security.minHobby
    val maxHobby = Settings.security.maxHobby
    val length = Random.nextInt(maxHobby-minHobby)+minHobby
    val result = Random.alphanumeric.take(length).mkString // Fix me --- should be any string, not jsut alphanumeric.
    if (validHobby(result)) result else randomHobby
  }

  def randomEmail: String = {
    val userLength = Random.nextInt(10)+1
    val hostLength = Random.nextInt(10)+1
    val user = Random.alphanumeric.take(userLength).mkString
    val host = Random.alphanumeric.take(hostLength).mkString
    val result = user + "@" + host + ".com"
    if (validEmail(result)) result else randomEmail
  }

  def randomUser(withAddress: Boolean, confirmed: Boolean): (User, String) = {
    val name = randomName
    val password = randomPassword
    val addr = if (withAddress) Some(randomAddress) else None
    val email = randomEmail
    val result = User.create(name, addr, email, password, confirmed)
    result match {
      case Some(u) => u.validate; (u, password)
      case None    => throw DbError("Test user could not be created")
    }
  }

  def randomUsers(n: Int, withAddress: Boolean, confirmed: Boolean): List[(User, String)] = {
    n match {
      case 0      => List()
      case m: Int => randomUser(withAddress, confirmed) :: randomUsers(m-1, withAddress, confirmed)
    }
  }

  def randomLoggedInUser(withAddress: Boolean, confirmed: Boolean, admin: Boolean = false): (User, String) = {
    val (user, password) = randomUser(withAddress, confirmed)
    val s = Session()
    User.login(user.email, password, s.key) match {
      case Some(u) => 
        u.admin = admin
        (u, password)
      case None    => throw DbError("Test user could not be created")
    }
  }

  def addRandomHobby(user: User): String = {
    val hobby = randomHobby
    user.addHobby(hobby)

    hobby
  }

  def createFriends: (User, User) = {
    val user1 = randomUser(true, true)._1
    val user2 = randomUser(true, true)._1
    user1.requestFriendship(user2, Friendship)
    user2.acceptFriendRequest(user1, Friendship)
    (user1, user2)
  }
}