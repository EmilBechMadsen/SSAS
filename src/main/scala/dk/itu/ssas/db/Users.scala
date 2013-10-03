package dk.itu.ssas.db

protected trait Users {
  import scala.slick.driver.MySQLDriver.simple._
  import dk.itu.ssas.model.User

  protected object Users extends Table[User]("user") {
    def id       = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def name     = column[String]("name", O.NotNull)
    def address  = column[Option[String]]("address")
    def email    = column[String]("email", O.NotNull)
    def password = column[String]("password", O.NotNull)
    def salt     = column[String]("salt", O.NotNull)

    def * = id ~ name ~ address ~ email ~ password ~ salt <> ((id, name, address, email, password, salt) => User.apply(id, name, address, email, password, salt), User unapply _) 
    def forInsert = name ~ address ~ email ~ password ~ salt

    def uniqueEmail = index("idx_email", email, unique = true)
  }
}
