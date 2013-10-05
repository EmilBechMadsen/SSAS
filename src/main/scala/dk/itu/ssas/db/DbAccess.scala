package dk.itu.ssas.db

trait DbAccess 
  extends Friends
  with Hobbies
  with UserHobbies
  with Relationships
  with FriendRequests
  with Admins
  with Sessions
  with FormKeys
  with EmailConfirmations {
  import dk.itu.ssas.Settings
  import scala.slick.driver.MySQLDriver.simple._

  protected val Db = Database.forURL(Settings.db.dbString, 
                                     driver = Settings.db.dbDriver)

  protected val upper = SimpleFunction.unary[String, String]("upper")

  protected val ddl = Users.ddl ++ Friends.ddl ++ Admins.ddl ++ 
                      Sessions.ddl ++ FriendRequests.ddl ++ 
                      EmailConfirmations.ddl ++ Hobbies.ddl ++ 
                      UserHobbies.ddl ++ FormKeys.ddl
}
