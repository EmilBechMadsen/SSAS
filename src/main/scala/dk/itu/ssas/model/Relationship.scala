package dk.itu.ssas.model

sealed abstract class Relationship
case class Friendship() extends Relationship
case class Romance() extends Relationship