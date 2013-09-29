package dk.itu.ssas.db

protected trait EmailConfirmations extends Users {
  import scala.slick.driver.MySQLDriver.simple._
  import scala.slick.lifted.ForeignKeyAction
  import java.util.UUID

  case class EmailConfirmation(key: UUID, userId: Int)

  object EmailConfirmations extends Table [EmailConfirmation]("email_confirmation") {
    def guid         = column[UUID]("guid", O.PrimaryKey)
    def userId       = column[Int]("user", O.NotNull)

    def * = guid ~ userId <> (EmailConfirmation, EmailConfirmation unapply _)

    def user         = foreignKey("fk_email_confirmation_user", userId, Users)(_.id,onDelete = ForeignKeyAction.Cascade)
  }
}
