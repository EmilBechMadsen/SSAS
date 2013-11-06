package dk.itu.ssas.page

object ViewRequestsPage extends LoggedInPage {
  import dk.itu.ssas.page.request._
  import dk.itu.ssas.model._
  import dk.itu.ssas.Settings.baseUrl

  type RequestType = ViewRequestsPageRequest

  private def requestEntry(entry: (User, Relationship), kind: Int, key: Key): HTML = {
    val user = entry._1
    val rel = entry._2
    s"""
    <tr class="listEntryRow listEntryColor${kind}">
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
    requests mapi {
      case (req, i) => requestEntry(req, if (i % 2 == 0) 1; else 2, key) 
    } mkString("\n")
  }

  def content(request: ViewRequestsPageRequest, key: Key): HTML = {
    s"""
    <div id="requestsBox">
      <div id="requestsCaption">
        Pending Requests
      </div>
      <div id="requestsListBox">
        <table cellspacing="0" style="width: 100%;">
          ${requestsToHTML(request.user.friendRequests, key)}
        </table>
      </div>
    </div>
    """
  }
}
