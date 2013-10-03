package dk.itu.ssas.db

protected trait Friends extends Users with Relationships {
  import scala.slick.driver.MySQLDriver.simple._
  import scala.slick.lifted.ForeignKeyAction
  import dk.itu.ssas.model.{User, Relationship, Friendship}

  protected case class Friend(user1Id: Int, user2Id: Int, relationship: Relationship)

  protected object Friends extends Table[Friend]("friend") {
    def user1Id      = column[Int]("user1")
    def user2Id      = column[Int]("user2")
    def relationship = column[Relationship]("relationship", O.NotNull, O.Default(Friendship))

    def * = user1Id ~ user2Id ~ relationship <> (Friend, Friend unapply _)

    def user1        = foreignKey("fk_friend_user1", user1Id, Users)(_.id, onDelete = ForeignKeyAction.Cascade)
    def user2        = foreignKey("fk_friend_user2", user2Id, Users)(_.id, onDelete = ForeignKeyAction.Cascade)

    def pk           = primaryKey("pk_friend", (user1Id, user2Id))
  }
}
