package gitbucket.core.util

import com.typesafe.config.ConfigFactory
import java.io.File

import Directory._
import ConfigUtil._
import com.github.takezoe.slick.blocking.{BlockingH2Driver, BlockingJdbcProfile, BlockingMySQLDriver}
import liquibase.database.AbstractJdbcDatabase
import liquibase.database.core.{H2Database, MariaDBDatabase, MySQLDatabase, PostgresDatabase}
import org.apache.commons.io.FileUtils

import scala.reflect.ClassTag

object DatabaseConfig {

  private lazy val config = {
    val file = new File(GitBucketHome, "database.conf")
    if (!file.exists) {
      FileUtils.write(
        file,
        """db {
          |  url = "jdbc:h2:${DatabaseHome}"
          |  user = "sa"
          |  password = "sa"
          |#  connectionTimeout = 30000
          |#  idleTimeout = 600000
          |#  maxLifetime = 1800000
          |#  minimumIdle = 10
          |#  maximumPoolSize = 10
          |}
          |""".stripMargin,
        "UTF-8"
      )
    }
    ConfigFactory.parseFile(file)
  }

  private lazy val dbUrl = getValue("db.url", config.getString) // config.getString("db.url")

  def url(directory: Option[String]): String = {
    val sb = new StringBuilder()
    sb.append(dbUrl)
    if (dbUrl.startsWith("jdbc:mysql:") && dbUrl.indexOf("permitMysqlScheme") == -1) {
      if (dbUrl.indexOf("?") == -1) {
        sb.append("?permitMysqlScheme")
      } else {
        sb.append("&permitMysqlScheme")
      }
    }
    sb.toString().replace("${DatabaseHome}", directory.getOrElse(DatabaseHome))
  }

  lazy val url: String = url(None)
  lazy val user: String = getValue("db.user", config.getString)
  lazy val password: String = getValue("db.password", config.getString)
  lazy val jdbcDriver: String = DatabaseType(url).jdbcDriver
  lazy val slickDriver: BlockingJdbcProfile = DatabaseType(url).slickDriver
  lazy val liquiDriver: AbstractJdbcDatabase = DatabaseType(url).liquiDriver
  lazy val connectionTimeout: Option[Long] = getOptionValue("db.connectionTimeout", config.getLong)
  lazy val idleTimeout: Option[Long] = getOptionValue("db.idleTimeout", config.getLong)
  lazy val maxLifetime: Option[Long] = getOptionValue("db.maxLifetime", config.getLong)
  lazy val minimumIdle: Option[Int] = getOptionValue("db.minimumIdle", config.getInt)
  lazy val maximumPoolSize: Option[Int] = getOptionValue("db.maximumPoolSize", config.getInt)

  private def getValue[T: ClassTag](path: String, f: String => T): T = {
    getConfigValue(path).getOrElse(f(path))
  }

  private def getOptionValue[T: ClassTag](path: String, f: String => T): Option[T] = {
    getConfigValue(path).orElse {
      if (config.hasPath(path)) Some(f(path)) else None
    }
  }

}

sealed trait DatabaseType {
  val jdbcDriver: String
  val slickDriver: BlockingJdbcProfile
  val liquiDriver: AbstractJdbcDatabase
}

object DatabaseType {

  def apply(url: String): DatabaseType = {
    if (url.startsWith("jdbc:h2:")) {
      H2
    } else if (url.startsWith("jdbc:mysql:")) {
      MySQL
    } else if (url.startsWith("jdbc:mariadb:")) {
      MariaDb
    } else if (url.startsWith("jdbc:postgresql:")) {
      PostgreSQL
    } else {
      throw new IllegalArgumentException(s"${url} is not supported.")
    }
  }

  object H2 extends DatabaseType {
    val jdbcDriver = "org.h2.Driver"
    val slickDriver: BlockingJdbcProfile = BlockingH2Driver
    val liquiDriver: AbstractJdbcDatabase = new H2Database()
  }

  object MySQL extends DatabaseType {
    val jdbcDriver = "org.mariadb.jdbc.Driver"
    val slickDriver: BlockingJdbcProfile = BlockingMySQLDriver
    val liquiDriver: AbstractJdbcDatabase = new MySQLDatabase()
  }

  object MariaDb extends DatabaseType {
    val jdbcDriver = "org.mariadb.jdbc.Driver"
    val slickDriver: BlockingJdbcProfile = BlockingMySQLDriver
    val liquiDriver: AbstractJdbcDatabase = new MariaDBDatabase()
  }

  object PostgreSQL extends DatabaseType {
    val jdbcDriver = "org.postgresql.Driver2"
    val slickDriver: BlockingJdbcProfile = BlockingPostgresDriver
    val liquiDriver: AbstractJdbcDatabase = new PostgresDatabase()
  }

  object BlockingPostgresDriver extends slick.jdbc.PostgresProfile with BlockingJdbcProfile {
    override def quoteIdentifier(id: String): String = {
      val s = new StringBuilder(id.length + 4) append '"'
      for (c <- id) if (c == '"') s append "\"\"" else s append c.toLower
      (s append '"').toString
    }
  }
}

/**
 * Shared connections to the auxiliary NoSQL/graph stores used by the audit-log,
 * search-cache and permission-graph features (MongoDB via the official Scala driver,
 * MongoDB via ReactiveMongo, and Neo4j via neotypes).
 */
object MongoConnection {
  private val client: org.mongodb.scala.MongoClient = org.mongodb.scala.MongoClient("mongodb://127.0.0.1:27017")
  val database: org.mongodb.scala.MongoDatabase = client.getDatabase("gitbucket")
  val auditCollection: org.mongodb.scala.MongoCollection[org.mongodb.scala.Document] = database.getCollection("audit_cache")
}

object ReactiveMongoConnection {
  implicit val system: akka.actor.ActorSystem = akka.actor.ActorSystem("gitbucket-reactivemongo")
  implicit val ec: scala.concurrent.ExecutionContext = system.dispatcher

  private val driver: reactivemongo.api.AsyncDriver = reactivemongo.api.AsyncDriver()
  private val connection: reactivemongo.api.MongoConnection =
    scala.concurrent.Await.result(driver.connect(List("127.0.0.1:27017")), scala.concurrent.duration.Duration(10, "seconds"))
  private val db: reactivemongo.api.DB =
    scala.concurrent.Await.result(connection.database("gitbucket"), scala.concurrent.duration.Duration(10, "seconds"))
  val auditCollection: reactivemongo.api.bson.collection.BSONCollection = db.collection("audit_log")
}

object Neo4jConnection {
  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val driver = neotypes.GraphDatabase.asyncDriver[scala.concurrent.Future](
    "bolt://127.0.0.1:7687",
    org.neo4j.driver.AuthTokens.basic("neo4j", "TempAdminPass123")
  )
}
