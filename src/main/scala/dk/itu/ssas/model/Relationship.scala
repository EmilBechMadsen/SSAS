package dk.itu.ssas.model

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
  def apply(r: String): Relationship = {
    r.toUpperCase() match {
      case "FRIENDSHIP" => Friendship
      case "ROMANCE"    => Romance
      case "BROMANCE"   => Bromance
      case x            => throw new Exception(s"Could not deserialize $x") // FIXME: Proper exception
    }
  }
}
