package dk.itu.ssas

import akka.actor.{ Actor, ActorLogging, Props }
import dk.itu.ssas.db.DbAccess
import scala.language.postfixOps

sealed abstract class DbWorkerMessage
object CleanDb extends DbWorkerMessage

class DbWorker extends Actor with ActorLogging with DbAccess {
  import dk.itu.ssas.Settings.db.cleanPass
  import dk.itu.ssas.Settings.security.{ formKeyTimeout, sessionTimeout }
  import java.sql.Timestamp
  import scala.concurrent.duration._
  import scala.slick.driver.MySQLDriver.simple._
  import scala.slick.driver.MySQLDriver.simple.Database.threadLocalSession

  implicit val dispatcher = context.system.dispatcher

  override def preStart(): Unit = {
    context.system.scheduler.scheduleOnce(cleanPass minutes, self, CleanDb)
  }

  def receive = {
    case CleanDb => {
      cleanSessions()
      cleanFormKeys()
      context.system.scheduler.scheduleOnce(cleanPass minutes, self, CleanDb)
    }
  }

  def cleanSessions(): Unit = Db withSession {
    val expTime = System.currentTimeMillis() - sessionTimeout * 60000
    val exp     = new Timestamp(expTime)

    val ss = for {
      s <- Sessions if s.creation <= exp
    } yield s

    log.info(s"Deleting ${ss.list.length} sessions")

    ss delete;
  }

  def cleanFormKeys(): Unit = Db withSession {
    val expTime = System.currentTimeMillis() - formKeyTimeout * 60000
    val exp     = new Timestamp(expTime)

    val fks = for {
      fk <- FormKeys if fk.creation <= exp
    } yield fk

    log.info(s"Deleting ${fks.list.length} form keys")

    fks delete;
  }
}
