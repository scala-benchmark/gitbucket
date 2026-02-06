package gitbucket.core.util

import javax.servlet.http.HttpServletResponse

object HtmlResponseHelper {

  /**
   * Completes the HTTP response with the given HTML body.
   */
  def complete(response: HttpServletResponse, htmlBody: String): Unit = {
    response.setContentType("text/html; charset=utf-8")
    val writer = response.getWriter
    //CWE-79
    //SINK
    writer.write(htmlBody)
  }
}
