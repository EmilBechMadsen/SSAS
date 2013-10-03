package dk.itu.ssas.model

class RelationshipDeserializationException(s: String) 
extends Exception(s)

sealed abstract class Relationship
object Friendship extends Relationship {
  override def toString(): String = "FRIENDSHIP"
}
object Romance    extends Relationship {
  override def toString(): String = "ROMANCE"
}
object Bromance   extends Relationship {
  override def toString(): String = "BROMANCE"
}

object Relationship {
  /** Given a string, returns a relationship
    *
    * @param Parameter1 - blah blah
    * @return Return value - blah blah
    * 
    * @throws RelationshipDeserializationException 
    */
  def apply(r: String): Relationship = {
    r.toUpperCase() match {
      case "FRIENDSHIP" => Friendship
      case "ROMANCE"    => Romance
      case "BROMANCE"   => Bromance
      case x            => throw new RelationshipDeserializationException(s"Could not deserialize $x")
    }
  }
}
