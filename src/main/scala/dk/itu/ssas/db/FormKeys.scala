package dk.itu.ssas.db

protected trait FormKeys extends Sessions {
  import java.sql.Timestamp
  import java.util.UUID
  import scala.slick.driver.MySQLDriver.simple._
  import scala.slick.lifted.ForeignKeyAction

  protected case class FormKey(key: String, session: String, expiration: Timestamp)

  protected object FormKeys extends Table [FormKey]("formkey") {
    def key        = column[String]("form_key", O.PrimaryKey)
    def sessionKey = column[String]("session", O.NotNull)
    def expiration = column[Timestamp]("expiration", O.NotNull)

    def * = key ~ sessionKey ~ expiration <> (FormKey, FormKey unapply _)

    def session    = foreignKey("fk_formkey_session", sessionKey, Sessions)(_.key, onDelete = ForeignKeyAction.Cascade)
  }
}
