package dk.itu.ssas.db

protected trait FriendRequests extends Users with Relationships {
  import scala.slick.driver.MySQLDriver.simple._
  import scala.slick.lifted.ForeignKeyAction
  import dk.itu.ssas.model.{User, Relationship, Friendship}

  protected case class FriendRequest(fromUser: Int, toUser: Int, relationship: Relationship)

  protected object FriendRequests extends Table [FriendRequest]("friend_request") {
    def fromUserId   = column[Int]("from_user", O.NotNull)
    def toUserId     = column[Int]("to_user", O.NotNull)
    def relationship = column[Relationship]("relationship", O.NotNull, O.Default(Friendship))

    def * = fromUserId ~ toUserId ~ relationship <> (FriendRequest, FriendRequest unapply _)

    def fromUser     = foreignKey("fk_friend_request_user1", fromUserId, Users)(_.id, onDelete = ForeignKeyAction.Cascade)
    def toUser       = foreignKey("fk_friend_request_user2", toUserId, Users)(_.id, onDelete = ForeignKeyAction.Cascade)

    def pk           = primaryKey("pk_friend_request", (fromUserId, toUserId))
  }
}
