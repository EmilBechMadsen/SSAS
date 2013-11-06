package dk.itu.ssas.db

protected trait Hugs extends Users {
  import dk.itu.ssas.model.{ Hug, User }
  import java.sql.Timestamp
  import scala.slick.driver.MySQLDriver.simple._
  import scala.slick.lifted.ForeignKeyAction

  protected object Hugs extends Table[Hug]("hug") {
    def id           = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def seen         = column[Boolean]("seen", O.NotNull, O.Default(false))
    def time         = column[Timestamp]("time", O.NotNull)
    def fromUserId   = column[Int]("fromUser")
    def toUserId     = column[Int]("toUser")

    def * = id ~ seen ~ time ~ fromUserId ~ toUserId <> (Hug, Hug unapply _)

    def forInsert    = time ~ fromUserId ~ toUserId

    def fromUser     = foreignKey("fk_hug_fromUser", fromUserId, Users)(_.id, onDelete = ForeignKeyAction.Cascade)
    def toUser       = foreignKey("fk_hug_toUser",   toUserId,   Users)(_.id, onDelete = ForeignKeyAction.Cascade)
  }
}
