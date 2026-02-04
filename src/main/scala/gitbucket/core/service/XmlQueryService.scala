package gitbucket.core.service

import kantan.xpath._
import kantan.xpath.implicits._

object XmlQueryService {

  /** Evaluates attacker-controlled XPath against the given XML document (e.g. app config). */
  def query(xpathExpr: String, xmlContent: String): String = {
    val queryResult = Query.compile[List[String]](xpathExpr)
    queryResult.fold(
      _ => "compile error",
      query => {
        //CWE-643
        //SINK
        xmlContent.evalXPath(query).fold(
          _ => "eval error",
          _.mkString(",")
        )
      }
    )
  }
}
