package dk.itu.ssas.db

protected trait ApiKeys {
  import dk.itu.ssas.model.ApiKey
  import java.util.UUID
  import scala.slick.driver.MySQLDriver.simple._
  import scala.slick.lifted.ForeignKeyAction

  protected object ApiKeys extends Table[ApiKey]("api_key") {
    def key      = column[String]("key", O.PrimaryKey)
    def revoked  = column[Boolean]("revoked", O.NotNull, O.Default(false))

    def * = key ~ revoked <> ((key, revoked) => ApiKey.apply(key, revoked), ApiKey unapply _)
  }
}
