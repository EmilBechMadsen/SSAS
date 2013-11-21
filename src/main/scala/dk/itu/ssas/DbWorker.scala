package dk.itu.ssas

import akka.actor.{ Actor, ActorLogging, Props }
import dk.itu.ssas.db.DbAccess
import scala.language.postfixOps

sealed abstract class DbWorkerMessage
object CleanDb extends DbWorkerMessage

class DbWorker extends Actor with ActorLogging with DbAccess {
  import dk.itu.ssas.Settings.db.cleanPass
  import dk.itu.ssas.Settings.security.{ formKeyTimeout, sessionTimeout, confirmationTimeout }
  import java.sql.Timestamp
  import scala.concurrent.duration._
  import scala.slick.driver.MySQLDriver.simple._
  import scala.slick.driver.MySQLDriver.simple.Database.threadLocalSession

  private val d = context.system.dispatcher
  private val s = context.system.scheduler

  override def preStart(): Unit = schedule()

  def receive = {
    case CleanDb => {
      cleanSessions()
      cleanFormKeys()
      cleanConfirmations()
      schedule()
    }
  }

  private def schedule(): Unit = {
    s.scheduleOnce(cleanPass minutes, self, CleanDb)(d)
  }

  private def cleanConfirmations(): Unit = Db withSession {
    val expTime = System.currentTimeMillis() - confirmationTimeout * 60000
    val exp = new Timestamp(expTime)

    val cs = for {
      c <- EmailConfirmations if c.timestamp <= exp
    } yield c

    val count = cs.list.length
    if (count > 0) log.info(s"Deleting $count EmailConfirmations (Expired)")

    cs delete;
  }

  private def cleanSessions(): Unit = Db withSession {
    val expTime = System.currentTimeMillis() - sessionTimeout * 60000
    val exp = new Timestamp(expTime)

    val ss = for {
      s <- Sessions if s.creation <= exp
    } yield s

    val c = ss.list.length
    if (c > 0) log.info(s"Deleting $c sessions")

    ss delete;
  }

  private def cleanFormKeys(): Unit = Db withSession {
    val expTime = System.currentTimeMillis() - formKeyTimeout * 60000
    val exp = new Timestamp(expTime)

    val fks = for {
      fk <- FormKeys if fk.creation <= exp
    } yield fk

    val c = fks.list.length
    if (c > 0) log.info(s"Deleting $c form keys")

    fks delete;
  }
}
