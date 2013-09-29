package dk.itu.ssas.db

protected trait UserHobbies extends Users with Hobbies {
  import scala.slick.driver.MySQLDriver.simple._
  import scala.slick.lifted.ForeignKeyAction

  case class UserHobby(userId: Int, hobbyId: Int)

  object UserHobbies extends Table[UserHobby]("user_hobby") {
    def userId       = column[Int]("user", O.NotNull)
    def hobbyId      = column[Int]("hobby", O.NotNull)

    def * = userId ~ hobbyId <> (UserHobby, UserHobby unapply _)

    def user         = foreignKey("fk_user_hobby_user", userId, Users)(_.id, onDelete = ForeignKeyAction.Cascade)
    def hobby        = foreignKey("fk_user_hobby_hobby", hobbyId, Hobbies)(_.id, onDelete = ForeignKeyAction.Cascade)

    def pk           = primaryKey("pk_friend_request", (userId, hobbyId))
  }
}
