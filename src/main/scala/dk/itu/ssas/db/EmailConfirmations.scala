package dk.itu.ssas.db

protected trait EmailConfirmations extends Users {
  import scala.slick.driver.MySQLDriver.simple._
  import scala.slick.lifted.ForeignKeyAction
  import java.util.UUID
  import java.sql.Timestamp
  import java.util.Calendar

  protected case class EmailConfirmation(key: String, userId: Int, timestamp: Timestamp)

  protected object EmailConfirmations extends Table [EmailConfirmation]("email_confirmation") {
    def guid         = column[String]("guid", O.PrimaryKey)
    def userId       = column[Int]("user", O.NotNull)
    def timestamp    = column[Timestamp]("timestamp", O.NotNull)

    def * = guid ~ userId ~ timestamp <> (EmailConfirmation, EmailConfirmation unapply _)

    def user         = foreignKey("fk_email_confirmation_user", userId, Users)(_.id,onDelete = ForeignKeyAction.Cascade)
  }
}
