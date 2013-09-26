package dk.itu.ssas.model

import dk.itu.ssas.db.DbModels
import dk.itu.ssas.Settings
import java.util.UUID
import scala.language.postfixOps
import scala.slick.driver.MySQLDriver.simple._
import scala.slick.driver.MySQLDriver.simple.Database.threadLocalSession

object User extends DbModels {
  import dk.itu.ssas.Security

  /** Returns a user if the email and password matches 
    *
    * @param email - The users email
    * @param password - The users password
    * @return Maybe a user
    */
  def apply(email: String, password: String): Option[User] = {
    val user = (for (u <- Users if u.email === email) yield u) firstOption

    user match {
      case Some(u) => Security.checkPassword(password, u.password, u.salt) match {
        case true  => user
        case false => None
      }
      case None => None
    }   
  }

  /** Returns a user if the id exists
    *
    * @param id - The id of the user
    * @return Maybe a user
    */
  def apply(id: Int): Option[User] = Database.forURL(Settings.dbString, driver = Settings.dbDriver) withSession {
    (for (u <- Users if u.id === id) yield u) firstOption
  }

  /** Returns a user if the key matches either a session of email confirmation key
    *
    * @param key - The session of email confirmation key
    * @return May a user
    */
  def apply(key: UUID): Option[User] = Database.forURL(Settings.dbString, driver = Settings.dbDriver) withSession {
    val user = (for {
      s <- Sessions if s.key    === key
      u <- Users    if s.userId === u.id
    } yield u) firstOption

    user match {
      case Some(u) => Some(u)
      case None    => (for {
        e <- EmailConfirmations if e.guid   === key
        u <- Users              if e.userId === u.id
        } yield u) firstOption
    }
  }

  /** Creates a new user in the database with the specified data
    *
    * @param name - The user's real name
    * @param address - The user's address
    * @param email - The user's email
    * @param password - The user's password
    * @return The created user
    */
  def createUser(name: String, address: Option[String], email: String, password: String): Option[User] = {
    val (hashedPw, salt) = Security.newPassword(password)
    val id = Users.forInsert returning Users.id insert (name, address, email, hashedPw, salt)

    User(id)
  }
}

case class User(
      val id: Int, 
      val name: String, 
      val address: Option[String],
      val email: String,
      val password: String,
      val salt: String)
    extends DbModels {
  /** Returns the user's friends
    *
    * @return A map from the relationship to the user
    */
  def friends: Map[Relationship, User] = {
    val fs = for {
      f <- Friends if  f.user1Id === id   || f.user2Id === id
      u <- Users   if (f.user1Id === u.id || f.user2Id === u.id) && !(u.id === id)
    } yield (f.relationship, u)

    (for ((r, u) <- fs.list) yield Relationship(r) -> u) toMap
  }

  /** Returns the outstanding friend requests for the user
    *
    * @return A map from the relationship to the user
    */
  def friendRequests: Map[Relationship, User] = {
    val fs = for {
      f <- FriendRequests if f.toUserId   === id
      u <- Users          if f.fromUserId === u.id
    } yield (f.relationship, u)

    (for ((r, u) <- fs.list) yield Relationship(r) -> u) toMap
  }

  /** Returns the a list of the users hobbies
    *
    * @return A list of the user's hobbies
    */
  def hobbies: List[Hobby] = {
    val hs = for {
      uh <- UserHobbies if uh.userId  === id
      h  <- Hobbies     if uh.hobbyId === h.id
    } yield h

    hs.list
  }

  /** Returns the admin status of the user
    *
    * @return Returns true if the user is an admin, false otherwise
    */
  def admin: Boolean = {
    (for (a <- Admins if (a.userId === id)) yield a.userId).list.nonEmpty
  }
}
