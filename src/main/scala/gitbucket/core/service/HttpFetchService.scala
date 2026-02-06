package gitbucket.core.service

import scalaj.http.Http

object HttpFetchService {

  def fetch(url: String): String = {
    //CWE-918
    //SINK
    Http(url).asString.body
  }
}
