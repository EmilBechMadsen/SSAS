package dk.itu.ssas.db

trait DbAccess 
  extends Friends
  with Hobbies
  with UserHobbies
  with Relationships
  with FriendRequests
  with Admins
  with Sessions
  with EmailConfirmations {
  import scala.slick.driver.MySQLDriver.simple._
  import dk.itu.ssas.Settings

  val Db = Database.forURL(Settings.db.dbString, driver = Settings.db.dbDriver)

  val upper = SimpleFunction.unary[String, String]("upper")

  val ddl = Users.ddl ++ Friends.ddl ++ Admins.ddl ++ Sessions.ddl ++ 
            FriendRequests.ddl ++ EmailConfirmations.ddl ++ 
            Hobbies.ddl ++ UserHobbies.ddl
}
