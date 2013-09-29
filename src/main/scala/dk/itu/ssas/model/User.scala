package dk.itu.ssas.model

import dk.itu.ssas.db.DbAccess
import dk.itu.ssas.Settings
import java.util.UUID
import scala.language.postfixOps
import scala.slick.driver.MySQLDriver.simple._
import scala.slick.driver.MySQLDriver.simple.Database.threadLocalSession

object User extends UserExceptions with DbAccess {
  import dk.itu.ssas.Security

  /** Returns a user if the email and password matches 
    *
    * @param email - The users email
    * @param password - The users password
    * @return Maybe a user
    */
  def login(email: String, password: String): Option[User] = Db withSession {
    val user = (for (u <- Users if u.email === email) yield u) firstOption

    user match {
      case Some(u) => Security.checkPassword(password, u.password, u._salt) match {
        case true  => {
          val q = (for (s <- Sessions if s.userId === u.id) yield s)

          val s = Session(UUID.randomUUID(), u.id)

          q firstOption match {
            case Some(s) => q update s // The user is already logged in, give him a new session
            case None    => Sessions insert s // The user is not logged in
          }

          user
        }
        case false => None // Wrong password
      }
      case None => None // No such user
    }
  }

  /** Returns a user if the id exists
    *
    * @param id - The id of the user
    * @return Maybe a user
    */
  def apply(id: Int): Option[User] = Db withSession {
    (for (u <- Users if u.id === id) yield u) firstOption
  }

  /** Returns a user if the key matches either a session of email confirmation key
    *
    * @param key - The session of email confirmation key
    * @return May a user
    */
  def apply(key: UUID): Option[User] = Db withSession {
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
  def create(name: String, address: Option[String], email: String, password: String): Option[User] = Db withSession {
    (validEmail(email), validName(name), validPassword(password), validAddress(address)) match {
      case (true, true, true, true) => {
        val (hashedPw, salt) = Security.newPassword(password)
        val id = Users.forInsert returning Users.id insert (name, address, email, hashedPw, salt)

        User(id)
      }
      // FIXME: What about combinations?
      case (false, _, _, _) => throw new InvalidEmailException
      case (_, false, _, _) => throw new InvalidNameException
      case (_, _, false, _) => throw new InvalidPasswordException
      case (_, _, _, false) => throw new InvalidAddressException
      case _ => None
    }
  }

  def validAddress(a: Option[String]): Boolean = {
    import Settings.security._

    a match {
      case Some(a) => 
        a.length >= minAddr && a.length <= maxAddr && a.matches(addrWhitelist)
      case None =>
        true
    }
  }

  /** Checks an email address for validity
    *
    * @param e - The email address to check
    * @return Return true if valid, false otherwise
    */
  def validEmail(e: String): Boolean = {
    import org.apache.commons.validator.routines.EmailValidator
    val ev = EmailValidator.getInstance()
    ev.isValid(e)
  }

  def validName(n: String): Boolean = {
    import Settings.security._

    n.length >= minName && n.length <= maxName && n.matches(nameWhitelist)
  }

  def validPassword(p: String): Boolean = {
    import Settings.security._

    p.length >= minPassword && p.length <= maxPassword
  }

  def validHobby(h: String): Boolean = {
    import Settings.security._

    h.length >= minHobby && h.length <= maxHobby && h.matches(hobbyWhitelist)
  }
}

case class User(
      val id: Int, 
      private var _name: String, 
      private var _address: Option[String],
      private var _email: String,
      private var _password: String,
      private var _salt: String)
    extends DbAccess {
  import User._

  /** Logs a user out
    *
    */
  def logout(): Unit = Db withSession {
    val q = (for (s <- Sessions if s.userId === id) yield s)

    q firstOption match {
      case Some(s) => q delete
      case None    => {} // The user is already logged out
    }
  }

  /** Returns the user's friends
    *
    * @return A map from the relationship to the user
    */
  def friends: Map[Relationship, User] = Db withSession {
    val fs = for {
      f <- Friends if  f.user1Id === id   || f.user2Id === id
      u <- Users   if (f.user1Id === u.id || f.user2Id === u.id) && !(u.id === id)
    } yield (f.relationship, u)

    (for ((r, u) <- fs.list) yield r -> u) toMap
  }

  /** Returns the outstanding friend requests for the user
    *
    * @return A map from the relationship to the user
    */
  def friendRequests: Map[Relationship, User] = Db withSession {
    val fs = for {
      f <- FriendRequests if f.toUserId   === id
      u <- Users          if f.fromUserId === u.id
    } yield (f.relationship, u)

    (for ((r, u) <- fs.list) yield r -> u) toMap
  }

  /** Returns the a list of the users hobbies
    *
    * @return A list of the user's hobbies
    */
  def hobbies: List[String] = Db withSession {
    val hs = for {
      uh <- UserHobbies if uh.userId  === id
      h  <- Hobbies     if uh.hobbyId === h.id
    } yield h.name

    hs.list
  }

  def addHobby(h: String): Unit = Db withSession {
    if (!validHobby(h)) throw new InvalidHobbyException

    val hobby = for {
      hobby <- Hobbies.map(h => (h.id, upper(h.name))) if hobby._2 === h.toUpperCase()
    } yield hobby

    hobby firstOption match {
      case Some(h) => UserHobbies insert UserHobby(id, h._1)
      case None    => {
        val hId = Hobbies.forInsert returning Hobbies.id insert h
        UserHobbies insert UserHobby(id, hId)
      }
    }
  }

  def removeHobby(h: String): Unit = Db withSession {
    val hobby = for {
      hobby <- Hobbies.map(h => (h.id, upper(h.name))) if hobby._2 === h.toUpperCase()
    } yield hobby

    hobby firstOption match {
      case Some(h) => {
        val q = for {
          uH <- UserHobbies if uH.userId === id && uH.hobbyId === h._1
        } yield uH

        q delete
      }
      case None => {}
    }
  }

  def address: Option[String] = _address

  def address_=(a: Option[String]): Unit = Db withSession {
    val q = (for (u <- Users if u.id === id) yield u.address) 

    a match {
      case Some(ad) => if (validAddress(a)) q update a
      case None     => q update None
    }

    (for (u <- Users if u.id === id && u.address === a) yield u) firstOption match {
      case Some(user) => _address = a
      case None       => throw new InvalidAddressException
    }
  }

  /** Returns the admin status of the user
    *
    * @return Returns true if the user is an admin, false otherwise
    */
  def admin: Boolean = Db withSession {
    (for (a <- Admins if (a.userId === id)) yield a.userId).list.nonEmpty
  }

  def admin_=(admin: Boolean): Unit = Db withSession {
    val q = (for (a <- Admins if a.userId === id) yield a)

    (q firstOption, admin) match {
      case (Some(a), false) => q delete
      case (None,    true)  => Admins insert id
      case _                => {}
    }
  }

  def email: String = _email
  
  def email_=(e: String): Unit = Db withSession {
    if (validEmail(e)) (for (u <- Users if u.id === id) yield u.email) update e
    else throw new InvalidEmailException

    (for (u <- Users if u.id === id && u.email === e) yield u) firstOption match {
      case Some(user) => _email = e
      case None       => throw new DbError
    }
  }

  def name: String = _name

  def name_=(n: String): Unit = Db withSession {
    if (validName(n)) (for (u <- Users if u.id === id) yield u.name) update n
    else throw new InvalidNameException

    (for (u <- Users if u.id === id && u.name === n) yield u) firstOption match {
      case Some(user) => _name = n
      case None       => throw new DbError
    }
  }

  def password: String = _password

  def password_=(p: String): Unit = Db withSession {
    import dk.itu.ssas.Security

    val (hashedPw, salt) = Security.newPassword(p)

    if (validPassword(p)) {
      // FIXME: These should be 1 statement
      (for (u <- Users if u.id === id) yield u.password) update hashedPw
      (for (u <- Users if u.id === id) yield u.salt)     update salt
    }
    else throw new InvalidPasswordException

    (for (u <- Users if u.id === id && u.password === hashedPw && u.salt === salt) yield u) firstOption match {
      case Some(user) => {
        _password = hashedPw
        _salt     = salt
      }
      case None       => throw new DbError
    }
  }

  def session: Option[UUID] = Db withSession {
    (for (s <- Sessions if s.userId === id) yield s.key) firstOption
  }

  override def toString(): String = {
    s"User($id, $name, $address, $email)"
  }
}
