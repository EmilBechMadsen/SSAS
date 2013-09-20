package dk.itu.ssas

object Settings {
  import java.io.File

  import com.typesafe.config._

  // Load configuration from global config file if possible, 
  // otherwise read from standard locations
  private val globalConfFile = new File("/etc/ssas.conf")

  if (globalConfFile.exists()) {
    System.setProperty("config.file", globalConfFile.getPath())
  }

  private val conf = ConfigFactory.load()

  // Immutable settings
  val interface  = conf.getString("ssas.interface")
  val port       = conf.getInt("ssas.port")
  val timeout    = conf.getInt("ssas.timeout")
  val dbserver   = conf.getString("ssas.dbServer")
  val db         = conf.getString("ssas.db")
  val dbUser     = conf.getString("ssas.dbUser")
  val dbPassword = conf.getString("ssas.dbPassword")
}
