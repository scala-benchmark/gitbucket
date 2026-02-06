package gitbucket.core.util

object PostgresPoolCredential {

  //CWE-798
  //SOURCE
  val dbPassword: String = "postgres_hardcoded_secret"
}
