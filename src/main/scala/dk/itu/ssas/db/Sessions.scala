package dk.itu.ssas.db

protected trait Sessions extends Users {
  import scala.slick.driver.MySQLDriver.simple._
  import scala.slick.lifted.ForeignKeyAction
  import java.util.UUID

  case class Session(key: UUID, userId: Int)

  object Sessions extends Table [Session]("session") {
    def key          = column[UUID]("session_key", O.PrimaryKey)
    def userId       = column[Int]("user", O.NotNull)

    def * = key ~ userId <> (Session, Session unapply _)

    def user         = foreignKey("fk_session_user", userId, Users)(_.id, onDelete = ForeignKeyAction.Cascade)
  }
}
