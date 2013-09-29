package dk.itu.ssas.db

protected trait Admins extends Users {
  import scala.slick.driver.MySQLDriver.simple._
  import scala.slick.lifted.ForeignKeyAction

  object Admins extends Table[(Int)]("admin") {
    def userId       = column[Int]("user", O.PrimaryKey)

    def * = userId

    def user = foreignKey("fk_admin_user", userId, Users)(_.id, onDelete = ForeignKeyAction.Cascade)
  }
}
