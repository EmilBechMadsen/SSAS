package dk.itu.ssas.db

trait DbModels {
  import scala.slick.driver.MySQLDriver.simple._
  import java.util.UUID

  import dk.itu.ssas.model.User

  object Users extends Table[User]("USER") {
    def id       = column[Int]("ID", O.PrimaryKey, O.AutoInc) // This is the primary key column
    def name     = column[String]("NAME")
    def address  = column[Option[String]]("ADDRESS")
    def email    = column[String]("EMAIL")
    def password = column[String]("PASSWORD")
    def salt     = column[String]("SALT")

    def * = id ~ name ~ address ~ email ~ password ~ salt <> ((id, name, address, email, password, salt) => User.apply(id, name, address, email, password, salt), User.unapply _)
    def forInsert = name ~ address ~ email ~ password ~ salt
  }

  case class Friend(user1Id: Int, user2Id: Int, relationship: String)

  object Friends extends Table[Friend]("FRIEND") {
    def user1Id      = column[Int]("USER1")
    def user2Id      = column[Int]("USER2")
    def relationship = column[String]("RELATIONSHIP")

    def * = user1Id ~ user2Id ~ relationship <> (Friend, Friend unapply _)

    def user1        = foreignKey("FK_FRIEND_USER1", user1Id, Users)(_.id)
    def user2        = foreignKey("FK_FRIEND_USER2", user2Id, Users)(_.id)

    def pk           = primaryKey("pk_FRIEND", (user1Id, user2Id))
  }

  object Admins extends Table[(Int)]("ADMIN") {
    def userId       = column[Int]("USER", O.PrimaryKey)

    def * = userId

    def user = foreignKey("FK_ADMIN_USER", userId, Users)(_.id)
  }

  case class Session(key: UUID, userId: Int)

  object Sessions extends Table [Session]("SESSION") {
    def key          = column[UUID]("SESSION_KEY", O.PrimaryKey)
    def userId       = column[Int]("USER")

    def * = key ~ userId <> (Session, Session unapply _)

    def user         = foreignKey("FK_SESSION_USER", userId, Users)(_.id)
  }

  case class FriendRequest(fromUserId: Int, toUserId: Int, relationship: String)

  object FriendRequests extends Table [FriendRequest]("FRIEND_REQUEST") {
    def fromUserId   = column[Int]("FROM_USER")
    def toUserId     = column[Int]("TO_USER")
    def relationship = column[String]("RELATIONSHIP")

    def * = fromUserId ~ toUserId ~ relationship <> (FriendRequest, FriendRequest unapply _)

    def fromUser     = foreignKey("FK_FRIEND_REQUEST_USER1", fromUserId, Users)(_.id)
    def toUser       = foreignKey("FK_FRIEND_REQUEST_USER2", toUserId, Users)(_.id)

    def pk           = primaryKey("pk_FRIEND_REQUEST", (fromUserId, toUserId))
  }

  case class EmailConfirmation(key: UUID, userId: Int)

  object EmailConfirmations extends Table [EmailConfirmation]("SESSION") {
    def guid         = column[UUID]("GUID", O.PrimaryKey)
    def userId       = column[Int]("USER")

    def * = guid ~ userId <> (EmailConfirmation, EmailConfirmation unapply _)

    def user         = foreignKey("FK_EMAIL_CONFIRMATION_USER", userId, Users)(_.id)
  }

  case class Hobby(id: Int, name: String)

  object Hobbies extends Table[Hobby]("HOBBY") {
    def id           = column[Int]("ID", O.PrimaryKey, O.AutoInc)
    def name         = column[String]("NAME")

    def * = id ~ name <> (Hobby, Hobby unapply _)
  }

  case class UserHobby(userId: Int, hobbyId: Int)

  object UserHobbies extends Table[UserHobby]("USER_HOBBY") {
    def userId       = column[Int]("USER")
    def hobbyId      = column[Int]("HOBBY")

    def * = userId ~ hobbyId <> (UserHobby, UserHobby unapply _)

    def user         = foreignKey("FK_USER_HOBBY_USER", userId, Users)(_.id)
    def hobby        = foreignKey("FK_USER_HOBBY_HOBBY", hobbyId, Hobbies)(_.id)

    def pk           = primaryKey("pk_FRIEND_REQUEST", (userId, hobbyId))
  }
}
