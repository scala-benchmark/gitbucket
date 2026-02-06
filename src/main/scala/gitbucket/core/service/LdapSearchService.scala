package gitbucket.core.service

import com.unboundid.ldap.sdk._
import gitbucket.core.service.SystemSettingsService.{DefaultLdapPort, Ldap}

import scala.jdk.CollectionConverters._

/**
 * LDAP search using filter.
 * Uses com.unboundid.ldap.sdk.LDAPConnection.search with tainted filter (CWE-90).
 */
object LdapSearchService {

  def search(ldapSettings: Ldap, filter: String): Either[String, Seq[String]] = {
    val conn = new LDAPConnection(ldapSettings.host, ldapSettings.port.getOrElse(DefaultLdapPort))
    try {
      conn.bind(ldapSettings.bindDN.getOrElse(""), ldapSettings.bindPassword.getOrElse(""))
      val baseDN = ldapSettings.baseDN
      //CWE-90
      //SINK
      val result = conn.search(baseDN, SearchScope.SUB, filter)
      val dns = result.getSearchEntries.asScala.map(_.getDN).toSeq
      Right(dns)
    } catch {
      case e: Exception =>
        if (conn.isConnected) conn.close()
        Left(e.toString)
    }
  }
}
