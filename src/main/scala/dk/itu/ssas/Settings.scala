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
  val interface     = conf.getString("ssas.interface")
  val port          = conf.getInt("ssas.port")
  val timeout       = conf.getInt("ssas.timeout")
  val baseUrl       = conf.getString("ssas.baseUrl")
  val staticBaseUrl = conf.getString("ssas.staticBaseUrl")
  val siteName      = conf.getString("ssas.siteName")

  object email {
    val host    = conf.getString("ssas.email.host")
    val address = conf.getString("ssas.email.address")
  }

  object db {
    val dbServer   = conf.getString("ssas.db.dbServer")
    val db         = conf.getString("ssas.db.db")
    val dbUser     = conf.getString("ssas.db.dbUser")
    val dbPassword = conf.getString("ssas.db.dbPassword")
    val dbString   = s"jdbc:mysql://$dbServer/$db?user=$dbUser&password=$dbPassword"
    val dbDriver   = "com.mysql.jdbc.Driver"
    val cleanPass  = conf.getLong("ssas.db.cleanPass")
  }

  object security {
    val ssl            = conf.getBoolean("ssas.security.ssl")
    val minPassword    = conf.getInt("ssas.security.minPassword")
    val maxPassword    = conf.getInt("ssas.security.maxPassword")
    val minName        = conf.getInt("ssas.security.minName")
    val maxName        = conf.getInt("ssas.security.maxName")
    val nameWhitelist  = conf.getString("ssas.security.nameWhitelist")
    val minAddr        = conf.getInt("ssas.security.minAddr")
    val maxAddr        = conf.getInt("ssas.security.maxAddr")
    val addrWhitelist  = conf.getString("ssas.security.addrWhitelist")
    val minHobby       = conf.getInt("ssas.security.minHobby")
    val maxHobby       = conf.getInt("ssas.security.maxHobby")
    val hobbyWhitelist = conf.getString("ssas.security.hobbyWhitelist")
    val formKeyTimeout = conf.getLong("ssas.security.formKeyTimeout")
    val sessionTimeout = conf.getLong("ssas.security.sessionTimeout")
    val confirmationTimeout = conf.getLong("ssas.security.confirmationTimeout")
  }
}
