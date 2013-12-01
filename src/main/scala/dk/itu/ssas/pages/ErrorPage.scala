package dk.itu.ssas.page

object ErrorPage extends WebPage {
  import dk.itu.ssas.model._
  import dk.itu.ssas.page.request._

  type RequestType = ErrorRequest

  def content(request: ErrorRequest, u: Option[User], key: Key): HTML = {
    s"""
    <div id="errorBox" class="content" style="width:30%;margin-left:auto;margin-right:auto;">
      <div class="header">
        <div id="caption">
          <h2>Error</h2>
        </div>
      </div>
      <div id="contentBody" style="padding:5px;">
        ${request.msg.html}
      </div>
    </div>
    """
  }
}
