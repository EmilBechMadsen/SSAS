package dk.itu.ssas.db

trait DbModels {
  import scala.slick.driver.MySQLDriver.simple._
  import scala.slick.lifted.ForeignKeyAction
  import java.util.UUID

  import dk.itu.ssas.model._

  implicit val relationshipTypeMapper = MappedTypeMapper.base[Relationship, String](
    { r =>
      r.toString()
    }, 
    { s =>
      Relationship(s)
    }
  )

  object Users extends Table[User]("user") {
    def id       = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def name     = column[String]("name", O.NotNull)
    def address  = column[Option[String]]("address")
    def email    = column[String]("email", O.NotNull)
    def password = column[String]("password", O.NotNull)
    def salt     = column[String]("salt", O.NotNull)

    def * = id ~ name ~ address ~ email ~ password ~ salt <> ((id, name, address, email, password, salt) => User.apply(id, name, address, email, password, salt), User unapply _) 
    def forInsert = name ~ address ~ email ~ password ~ salt

    def uniqueEmail = index("idx_email", email)
  }

  case class Friend(user1Id: Int, user2Id: Int, relationship: Relationship)

  object Friends extends Table[Friend]("friend") {
    def user1Id      = column[Int]("user1")
    def user2Id      = column[Int]("user2")
    def relationship = column[Relationship]("relationship", O.NotNull, O.Default(Friendship))

    def * = user1Id ~ user2Id ~ relationship <> (Friend, Friend unapply _)

    def user1        = foreignKey("fk_friend_user1", user1Id, Users)(_.id, onDelete = ForeignKeyAction.Cascade)
    def user2        = foreignKey("fk_friend_user2", user2Id, Users)(_.id, onDelete = ForeignKeyAction.Cascade)

    def pk           = primaryKey("pk_friend", (user1Id, user2Id))
  }

  object Admins extends Table[(Int)]("admin") {
    def userId       = column[Int]("user", O.PrimaryKey)

    def * = userId

    def user = foreignKey("fk_admin_user", userId, Users)(_.id, onDelete = ForeignKeyAction.Cascade)
  }

  case class Session(key: UUID, userId: Int)

  object Sessions extends Table [Session]("session") {
    def key          = column[UUID]("session_key", O.PrimaryKey)
    def userId       = column[Int]("user", O.NotNull)

    def * = key ~ userId <> (Session, Session unapply _)

    def user         = foreignKey("fk_session_user", userId, Users)(_.id, onDelete = ForeignKeyAction.Cascade)
  }

  case class FriendRequest(fromUserId: Int, toUserId: Int, relationship: Relationship)

  object FriendRequests extends Table [FriendRequest]("friend_request") {
    def fromUserId   = column[Int]("from_user", O.NotNull)
    def toUserId     = column[Int]("to_user", O.NotNull)
    def relationship = column[Relationship]("relationship", O.NotNull, O.Default(Friendship))

    def * = fromUserId ~ toUserId ~ relationship <> (FriendRequest, FriendRequest unapply _)

    def fromUser     = foreignKey("fk_friend_request_user1", fromUserId, Users)(_.id, onDelete = ForeignKeyAction.Cascade)
    def toUser       = foreignKey("fk_friend_request_user2", toUserId, Users)(_.id, onDelete = ForeignKeyAction.Cascade)

    def pk           = primaryKey("pk_friend_request", (fromUserId, toUserId))
  }

  case class EmailConfirmation(key: UUID, userId: Int)

  object EmailConfirmations extends Table [EmailConfirmation]("email_confirmation") {
    def guid         = column[UUID]("guid", O.PrimaryKey)
    def userId       = column[Int]("user", O.NotNull)

    def * = guid ~ userId <> (EmailConfirmation, EmailConfirmation unapply _)

    def user         = foreignKey("fk_email_confirmation_user", userId, Users)(_.id,onDelete = ForeignKeyAction.Cascade)
  }

  case class Hobby(id: Int, name: String)

  object Hobbies extends Table[Hobby]("hobby") {
    def id           = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def name         = column[String]("name", O.NotNull)

    def * = id ~ name <> (Hobby, Hobby unapply _)
  }

  case class UserHobby(userId: Int, hobbyId: Int)

  object UserHobbies extends Table[UserHobby]("user_hobby") {
    def userId       = column[Int]("user", O.NotNull)
    def hobbyId      = column[Int]("hobby", O.NotNull)

    def * = userId ~ hobbyId <> (UserHobby, UserHobby unapply _)

    def user         = foreignKey("fk_user_hobby_user", userId, Users)(_.id, onDelete = ForeignKeyAction.Cascade)
    def hobby        = foreignKey("fk_user_hobby_hobby", hobbyId, Hobbies)(_.id, onDelete = ForeignKeyAction.Cascade)

    def pk           = primaryKey("pk_friend_request", (userId, hobbyId))
  }

  val ddl = Users.ddl ++ Friends.ddl ++ Admins.ddl ++ Sessions.ddl ++ 
            FriendRequests.ddl ++ EmailConfirmations.ddl ++ 
            Hobbies.ddl ++ UserHobbies.ddl
}
