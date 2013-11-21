package dk.itu.ssas.page

object ViewRequestsPage extends WebPage {
  import dk.itu.ssas.page.request._
  import dk.itu.ssas.model._
  import dk.itu.ssas.page.exception._
  import dk.itu.ssas.Settings.baseUrl

  type RequestType = NoRequest

  private def requestEntry(entry: (User, Relationship), key: Key): HTML = {
    val user = entry._1
    val rel = entry._2
    s"""
    <tr class="listEntryRow">
      <form action="$baseUrl/requests" method="POST">
        ${formKeyInput(key)}
        <input name="friendRequestId" type="hidden" value="${user.id}" />
        <input name="friendRequestKind" type="hidden" value="$rel" />
        <td class="requestsEntryName">${user.name.html}</td>
        <td class="requestsEntryStatus">${rel.prettyPrint}</td>
        <td class="requestsEntryButton">
          <input name="friendRequestAccept" class="styledSubmitButton" type="submit" value="Accept" />
          <input name="friendRequestReject" class="styledSubmitButton" type="submit" value="Reject" />
        </td>
      </form>
    </tr>
    """    
  }

  private def requestsToHTML(requests: Map[User, Relationship], key: Key): HTML = {
    requests map { req => requestEntry(req, key) } mkString("\n")
  }

  def content(request: NoRequest, u: Option[User], key: Key): HTML = {
    val user = u.getOrElse(throw NoUserException())
    s"""
    <div id="requestsBox" class="content">
      <div class="header">
        <div id="caption">
          <h2>Pending Requests</h2>
        </div>
      </div
      <div id="contentBody">
        <table cellspacing="0" style="padding-top: 10px; padding-bottom: 10px;">
          ${requestsToHTML(user.friendRequests, key)}
        </table>
      </div>
    </div>
    """
  }
}
