package dk.itu.ssas.db

protected trait Hobbies {
  import scala.slick.driver.MySQLDriver.simple._

  protected case class Hobby(id: Int, name: String)

  protected object Hobbies extends Table[Hobby]("hobby") {
    def id           = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def name         = column[String]("name", O.NotNull)

    def * = id ~ name <> (Hobby, Hobby unapply _)
    def forInsert = name

    def uniqueName = index("idx_name", name, unique = true)
  }
}
