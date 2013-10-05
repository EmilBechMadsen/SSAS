package dk.itu.ssas.db

protected trait Sessions extends Users {
  import dk.itu.ssas.model.Session
  import scala.slick.driver.MySQLDriver.simple._
  import scala.slick.lifted.ForeignKeyAction
  import java.util.UUID

  protected object Sessions extends Table [Session]("session") {
    def key          = column[String]("session_key", O.PrimaryKey)
    def userId       = column[Option[Int]]("user")

    def * = key ~ userId <> (
      (key, userId) => Session.apply(key, userId), 
      Session unapply _
    )

    def user         = foreignKey("fk_session_user", userId, Users)(_.id, onDelete = ForeignKeyAction.Cascade)
  }
}
