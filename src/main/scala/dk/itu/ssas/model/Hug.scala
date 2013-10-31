package dk.itu.ssas.model

import java.sql.Timestamp

protected case class Hug(id: Int, seen: Boolean, time: Timestamp, fromUserId: Int, toUserId: Int) {
  def fromUser: Option[User] = User(fromUserId)
  
  def toUser: Option[User] = User(toUserId)
}
