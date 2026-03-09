package gitbucket.core.plugin

import gitbucket.core.controller.Context
import gitbucket.core.service.RepositoryService
import gitbucket.core.view.Markdown
import gitbucket.core.view.helpers.urlLink
import play.twirl.api.Html

/**
 * A render engine to render content to HTML.
 */
trait Renderer {

  /**
   * Render the given request to HTML.
   */
  def render(request: RenderRequest, importData: Array[Byte] = Array.empty): Html

}

object MarkdownRenderer extends Renderer {
  override def render(request: RenderRequest, importData: Array[Byte] = Array.empty): Html = {
    if (request == null && importData.nonEmpty) {
      val result = Markdown.toHtml("", null, "", false, false, false, false, false, false, Nil, importData)(null)
      return Html(result)
    }
    import request.*
    Html(
      Markdown.toHtml(
        markdown = fileContent,
        repository = repository,
        branch = branch,
        enableWikiLink = enableWikiLink,
        enableRefsLink = enableRefsLink,
        enableAnchor = enableAnchor,
        enableLineBreaks = enableLineBreaks,
        enableTaskList = enableTaskList,
        hasWritePermission = hasWritePermission,
        importData = importData
      )(context)
    )
  }
}

object DefaultRenderer extends Renderer {
  override def render(request: RenderRequest, importData: Array[Byte] = Array.empty): Html = {
    Html(s"""<tt><pre class="plain">${urlLink(request.fileContent)}</pre></tt>""")
  }
}

case class RenderRequest(
  filePath: List[String],
  fileContent: String,
  branch: String,
  repository: RepositoryService.RepositoryInfo,
  enableWikiLink: Boolean,
  enableRefsLink: Boolean,
  enableAnchor: Boolean,
  enableLineBreaks: Boolean,
  enableTaskList: Boolean,
  hasWritePermission: Boolean,
  context: Context
)
