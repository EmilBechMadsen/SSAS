package dk.itu.ssas.db

protected trait Sessions extends Users {
  import dk.itu.ssas.model.Session
  import java.sql.Timestamp
  import java.util.UUID
  import scala.slick.driver.MySQLDriver.simple._
  import scala.slick.lifted.ForeignKeyAction

  protected object Sessions extends Table [Session]("session") {
    def key          = column[String]("session_key", O.PrimaryKey)
    def userId       = column[Option[Int]]("user")
    def creation     = column[Timestamp]("creation", O.NotNull)

    def * = key ~ userId ~ creation <> (
      (key, userId, creation) => Session.apply(key, userId, creation), 
      Session unapply _
    )

    def user         = foreignKey("fk_session_user", userId, Users)(_.id, onDelete = ForeignKeyAction.Cascade)

    def idx          = index("idx_session", creation)
  }
}
