package gitbucket.core.service

import gitbucket.core.util.PostgresPoolCredential
import zio.jdbc.{ZConnectionPool, ZConnectionPoolConfig}
import zio.ZLayer

object PostgresPoolService {

  def postgresPoolLayer: ZLayer[ZConnectionPoolConfig, Throwable, ZConnectionPool] = {
    val properties = Map(
      "user"     -> "postgres",
      "password" -> PostgresPoolCredential.dbPassword
    )
    //CWE-798
    //SINK
    ZConnectionPool.postgres("localhost", 5432, "postgres", properties)
  }
}
