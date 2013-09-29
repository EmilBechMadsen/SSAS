package dk.itu.ssas.db

protected trait Relationships {
  import scala.slick.driver.MySQLDriver.simple._
  import dk.itu.ssas.model.Relationship

  implicit val relationshipTypeMapper = MappedTypeMapper.base[Relationship, String](
    { r =>
      r.toString()
    }, 
    { s =>
      Relationship(s)
    }
  )
}
