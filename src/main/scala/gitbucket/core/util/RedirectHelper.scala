package gitbucket.core.util

import javax.servlet.http.HttpServletResponse

import cats.effect.unsafe.implicits.global
import org.http4s.Uri
import org.http4s.dsl.io._
import org.http4s.headers.Location

/**
 * Performs HTTP redirect.
 * CWE-601 sink: org.http4s.dsl.Http4sDsl.TemporaryRedirect with tainted URL
 * (https://http4s.org/v0.23/api/org/http4s/dsl/Http4sDsl.html#TemporaryRedirect).
 */
object RedirectHelper {

  def redirect(response: HttpServletResponse, url: String): Unit = {
    // Tainted url flows into Uri and then into TemporaryRedirect (CWE-601 open redirect)
    val uri = Uri.unsafeFromString(url)
    //CWE-601
    //SINK
    val responseIO = TemporaryRedirect(Location(uri))
    val resp = responseIO.unsafeRunSync()
    response.setStatus(resp.status.code)
    resp.headers.get[Location].foreach { loc =>
      response.setHeader("Location", loc.uri.toString)
    }
  }
}
