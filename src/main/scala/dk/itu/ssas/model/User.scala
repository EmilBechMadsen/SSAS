package dk.itu.ssas.model

import dk.itu.ssas.db.DbAccess
import dk.itu.ssas.Settings
import java.util.UUID
import scala.language.postfixOps
import scala.slick.driver.MySQLDriver.simple._
import scala.slick.driver.MySQLDriver.simple.Database.threadLocalSession

object User extends UserExceptions with DbAccess {
  import dk.itu.ssas.{ ConfirmationMail, Security, Server }
  import dk.itu.ssas.Validate._

  private val mailer = Server.mailer

  /** Returns a user if the id exists
    *
    * @param id - The id of the user
    * @return Maybe a user
    */
  def apply(id: Int): Option[User] = Db withSession {
    val user = (for {
      u <- Users if u.id === id
    } yield u) firstOption
    
    user match {
      case Some(u) => {
        u.validate
        user
      }
      case None => None
    }
  }




  /** Returns a user if the key matches either a session of email confirmation key
    *
    * @param key - The session of email confirmation key
    * @return May a user
    */
  def apply(key: UUID): Option[User] = Db withSession {
    val user = (for {
      s <- Sessions if s.key === key.toString()
      u <- Users    if s.userId === u.id
    } yield u) firstOption

    user match {
      case Some(u) => Some(u)
      case None => (for {
        e <- EmailConfirmations if e.guid === key.toString()
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
    * 
    * @throws InvalidEmailException
    * @throws InvalidNameException
    * @throws InvalidPasswordException
    * @throws InvalidAddressException
    * @throws ExistingEmailException
    */
  def create(name: String, address: Option[String], email: String, password: String): Option[User] = Db withSession {
    (validEmail(email), validName(name), validPassword(password), validAddress(address)) match {
      case (true, true, true, true) => {
        val uniqueEmail = (for {
          u <- Users if u.email === email
        } yield u).firstOption.isEmpty

        if (uniqueEmail) {
          val (hashedPw, salt) = Security.newPassword(password)
          val user = (name, address, email, hashedPw, salt)
          val id = Users.forInsert returning Users.id insert user

          val key = UUID.randomUUID()

          val ec = EmailConfirmation(key.toString(), id)
          EmailConfirmations insert ec

          mailer ! ConfirmationMail(email, name, key)

          User(id)
        } else {
          throw new ExistingEmailException
        }
      }
      // FIXME: What about combinations?
      case (false, _, _, _) => throw new InvalidEmailException
      case (_, false, _, _) => throw new InvalidNameException
      case (_, _, false, _) => throw new InvalidPasswordException
      case (_, _, _, false) => throw new InvalidAddressException
      case _                => None
    }
  }

  /** Returns a user if the email and password matches
    *
    * @param email - The users email
    * @param password - The users password
    * @return Maybe a user
    */
  def login(email: String, password: String, session: UUID): Option[User] = Db withSession {
    (for (u <- Users if u.email === email) yield u) firstOption match {
      case Some(u) => (u.checkPassword(password), u.isConfirmed) match {
        case (true, true) => {
          val q = (for (s <- Sessions if s.key === session.toString()) yield s.userId)

          q firstOption match {
            case Some(s) => {
              q update Some(u.id)
              Some(u)
            }
            case None    => None
          }
        }
        // Wrong password or unconfirmed
        case (_, _) => None
        // Fixme: throw exception for unconfirmed
      }
      case None => None // No such user
    }
  }

  /** A list of all users
    *
    * @return A list of all users
    */
  def all: List[User] = Db withSession {
    val users = for (u <- Users) yield u
    users.list
  }

  /** A list of all admins
    *
    * @return A list of all admins
    */
  def allAdmins: List[User] = Db withSession {
    val admins = for {
      a <- Admins
      u <- Users if (u.id === a.userId)
    } yield u
    admins.list
  }
}

/** A user
  *
  * @param id - The id for the user
  * @param _name - The name of the user
  * @param _address - The address of the user
  * @param _email - The email for the user
  * @param _password - The password for the user
  * @param _salt - The salt for the user
  */
case class User(
    val id: Int,
    private var _name: String,
    private var _address: Option[String],
    private var _email: String,
    private var _password: String,
    private var _salt: String)
  extends DbAccess {
  import User._
  import dk.itu.ssas.Validate._

  /** Deletes the user. *WARNING* this is permanent
    *
    */
  def delete(): Unit = Db withSession {
    (for (u <- Users if u.id === id) yield u) delete
  }

  def validate: Unit = {
    if (!validName(_name)) throw new InvalidNameException
    if (!validEmail(_email)) throw new InvalidEmailException
    if (!validAddress(_address)) throw new InvalidAddressException
  }
  /** Confirms a user
    *
    * @param key - The key used to confirm the user
    * @return Returns true if the user is confirmed, false otherwise
    */
  def confirm(key: UUID): Boolean = Db withSession {
    val ec = for {
      e <- EmailConfirmations if e.userId === id && e.guid === key.toString()
    } yield e

    ec delete

    (ec firstOption) isDefined
  }

  /** The confirmation status of the user
    *
    * @return True if the user is confirmed, false otherwise
    */
  def isConfirmed: Boolean = Db withSession {
    (for {
      ec <- EmailConfirmations if ec.userId === id
    } yield ec).firstOption.isEmpty
  }

  /** Logs a user out
    *
    */
  def logout(): Unit = Db withSession {
    val q = for (s <- Sessions if s.userId === id) yield s

    q firstOption match {
      case Some(s) => q delete
      case None    => {} // The user is already logged out
    }
  }

  /** Checks if a password is correct
    *
    * @param p - The password to check
    * @return Returns true if the password is correct
    */
  def checkPassword(p: String): Boolean = Db withSession {
    import dk.itu.ssas.Security

    Security.checkPassword(p, _password, _salt)
  }

  /** Searches for other users by name
    *
    * @param s - The search string
    * @return A list of users with names matching the search string
    */
  def search(s: String): List[User] = Db withSession {
    if (validName(s)) {
      val users = for {
        u <- Users if u.name like s"%$s%"
      } yield u

      users.list filter (u => u.id != id)
    } else List()
  }

  /** Returns the user's friends
    *
    * @return A map from the relationship to the user
    */
  def friends: Map[User, Relationship] = Db withSession {
    val fs = for {
      f <- Friends if f.user1Id === id || f.user2Id === id
      u <- Users if (f.user1Id === u.id || f.user2Id === u.id) && !(u.id === id)
    } yield (u, f.relationship)

    (for ((u, r) <- fs.list) yield u -> r) toMap
  }

  /** Checks if a user is a friend of this user
    *
    * @param u - The user to check
    * @return True if they are friends, false otherwise
    */
  def isFriend(u: User): Boolean = isFriend(u.id)

  /** Checks if a user is a friend of this user
    *
    * @param friendId - The id of the user to check
    * @return True if they are friends, false otherwise
    */
  def isFriend(friendId: Int): Boolean = Db withSession {
    getFriend(friendId) match {
      case Some(_) => true
      case None    => false
    }
  }

  private def getFriend(friendId: Int): Option[User] = Db withSession {
    (for {
      f <- Friends if (f.user1Id === id && f.user2Id === friendId) ||
                      (f.user1Id === friendId && f.user2Id === id)
      u <- Users if (u.id === f.user1Id || u.id === f.user2Id)
    } yield u) firstOption
  }

  /** Returns the outstanding friend requests for the user
    *
    * @return A map from the relationship to the user
    */
  def friendRequests: Map[User, Relationship] = Db withSession {
    val fs = for {
      f <- FriendRequests if f.toUserId === id
      u <- Users if f.fromUserId === u.id
    } yield (u, f.relationship)

    (for ((u, r) <- fs.list) yield u -> r) toMap
  }

  /** Accepts a relationship from a user
    *
    * @param u - The user to accept the request from
    * @param r - The relationship to accept
    * @return True if the users are now friends, false otherwise
    */
  def acceptFriendRequest(u: User, r: Relationship): Boolean = Db withSession {
    val fr = for (
      fr <- FriendRequests if fr.toUserId === id &&
        fr.fromUserId === u.id &&
        fr.relationship === r
    ) yield fr

    fr firstOption match {
      case Some(req) => {
        val f = for {
          f <- Friends if (f.user1Id === id && f.user2Id === req.fromUser) ||
                          (f.user1Id === req.fromUser && f.user2Id === id)
        } yield f.relationship

        f firstOption match {
          case Some(_) => { // Updating existing friendship
            f update req.relationship
            fr.delete
            true
          }
          case None => { // New friendship
            Friends insert Friend(req.fromUser, id, req.relationship)
            fr.delete
            true
          }
        }
      }
      case None => false // No such friend request FIXME: Exception
    }
  }

  /** Rejects a relationship request
    *
    * @param u - The user to reject
    * @param r - The relationship to reject
    */
  def rejectFriendRequest(u: User, r: Relationship): Unit = Db withSession {
    // Deletes all friend requests from the user, even if they are for 
    // different relationships
    (for (
      fr <- FriendRequests if fr.toUserId === id &&
                              fr.fromUserId === u.id
    ) yield fr) delete
  }

  /** Request a relationship from another user
    *
    * @param u - The user to request a relationship from
    * @param r - The relationship to request
    */
  def requestFriendship(u: User, r: Relationship): Unit = Db withSession {
    // Only send request if this is a new relationship
    if (!(isFriend(u) && friends.get(u) == Some(r)) &&
        u.id != id) {
      val fr = for {
        fr <- FriendRequests if (fr.fromUserId === u.id && fr.toUserId === id) ||
                                (fr.fromUserId === id && fr.toUserId === u.id)
      } yield fr

      fr firstOption match {
        // No existing friend requests
        case None => FriendRequests insert FriendRequest(id, u.id, r)
        case Some(f) => {
          // The other person has sent a friend request with the same relationship,
          // so we can just accept it
          if (f.relationship == r)
            acceptFriendRequest(u, r)
          else { // The other person has sent a friend request, but for a different
            // relationship
            fr.delete
            FriendRequests insert FriendRequest(id, u.id, r)
          }
        }
      }
    }
  }

  /** Removes a friend
    *
    * @param f - The friend to remove
    */
  def removeFriend(f: User): Unit = Db withSession {
    val fq = for {
      fr <- Friends if (fr.user1Id === f.id && fr.user2Id === id) ||
        (fr.user1Id === id && fr.user2Id === f.id)
    } yield fr

    fq delete
  }

  /** Returns the a list of the users hobbies
    *
    * @return A list of the user's hobbies
    */
  def hobbies: List[String] = Db withSession {
    val hobbies = (for {
      uh <- UserHobbies if uh.userId === id
      h <- Hobbies if uh.hobbyId === h.id
    } yield h.name) list

    if (!(hobbies forall (h => validHobby(h))))
      throw new InvalidHobbyException
    hobbies
  }

  /** Adds a hobby to a user's list of hobbies
    *
    * @param h - The hobby to add
    *
    * @throws InvalidHobbyException
    */
  def addHobby(h: String): Unit = Db withSession {
    if (!validHobby(h)) throw new InvalidHobbyException

    val hobby = for {
      hobby <- Hobbies.map(h => (h.id, upper(h.name))) if hobby._2 === h.toUpperCase()
    } yield hobby

    hobby firstOption match {
      case Some(h) => UserHobbies insert UserHobby(id, h._1)
      case None => {
        val hId = Hobbies.forInsert returning Hobbies.id insert h
        UserHobbies insert UserHobby(id, hId)
      }
    }
  }

  /** Removes a hobby from the user's list of hobbies
    *
    * @param h - The hobby to remove
    *
    * @throws InvalidHobbyException
    */
  def removeHobby(h: String): Unit = Db withSession {
    if (!validHobby(h)) throw new InvalidHobbyException

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

  /** The address of a user
    *
    * @return Maybe the user's address
    */
  def address: Option[String] = _address

  /** Sets the user's address
    *
    * @param a - The address to set
    *
    * @throws InvalidAddressException
    */
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

  /** The admin status of the user
    *
    * @return Returns true if the user is an admin, false otherwise
    */
  def admin: Boolean = Db withSession {
    (for (a <- Admins if (a.userId === id)) yield a.userId).list.nonEmpty
  }

  /** Sets the admin status of the user
    *
    * @param a - The admin status to set
    */
  def admin_=(a: Boolean): Unit = Db withSession {
    val q = (for (a <- Admins if a.userId === id) yield a)

    (q firstOption, a) match {
      case (Some(a), false) => q delete
      case (None, true)     => Admins insert id
      case _                => {}
    }
  }

  /** The email address for the user
    *
    * @return The email address of the user
    */
  def email: String = _email

  /** Sets the email address for the user
    *
    * @param e - The email address to set
    *
    * @throws InvalidEmailException
    * @throws DbError
    */
  def email_=(e: String): Unit = Db withSession {
    if (validEmail(e)) (for (u <- Users if u.id === id) yield u.email) update e
    else throw new InvalidEmailException

    (for (u <- Users if u.id === id && u.email === e) yield u) firstOption match {
      case Some(user) => _email = e
      case None       => throw new DbError
    }
  }

  /** The name of the user
    *
    * @return The name of the user
    */
  def name: String = _name

  /** Sets the name of the user
    *
    * @param n - The name to set
    *
    * @throws InvalidNameException
    * @throws DbError
    */
  def name_=(n: String): Unit = Db withSession {
    if (validName(n)) (for (u <- Users if u.id === id) yield u.name) update n
    else throw new InvalidNameException

    (for (u <- Users if u.id === id && u.name === n) yield u) firstOption match {
      case Some(user) => _name = n
      case None       => throw new DbError
    }
  }

  /** The hashed password for the user
    *
    * @return The hashed password for the user
    */
  def password: String = _password

  /** Sets the password for the user
    *
    * @param p - The password to set
    *
    * @throws InvalidPasswordException
    * @throws DbError
    */
  def password_=(p: String): Unit = Db withSession {
    import dk.itu.ssas.Security

    val (hashedPw, salt) = Security.newPassword(p)

    if (validPassword(p)) {
      // FIXME: These should be 1 statement
      (for (u <- Users if u.id === id) yield u.password) update hashedPw
      (for (u <- Users if u.id === id) yield u.salt) update salt
    } else throw new InvalidPasswordException

    (for (u <- Users if u.id === id && u.password === hashedPw && u.salt === salt) yield u) firstOption match {
      case Some(user) => {
        _password = hashedPw
        _salt = salt
      }
      case None => throw new DbError
    }
  }

  /** The session for the user
    *
    * @return The session for the user
    */
  def session: Option[UUID] = Db withSession {
    (for (s <- Sessions if s.userId === id) yield s.key) firstOption match {
      case Some(s) => Some(UUID.fromString(s))
      case None    => None
    }
  }

  override def toString(): String = {
    s"User($id, $name, $address, $email)"
  }
}
