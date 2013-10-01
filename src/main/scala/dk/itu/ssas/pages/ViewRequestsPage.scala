package dk.itu.ssas.page

object ViewRequestsPage extends LoggedInPage {
  import dk.itu.ssas.page.request._
  import dk.itu.ssas.model._

  type RequestType = ViewRequestsPageRequest

  private def requestEntry(entry: (User, Relationship), kind: Int, key: Int): HTML = {
    val user = entry._1
    val rel = entry._2
    s"""
    <tr class="listEntryRow listEntryColor${kind}">
      <form method="POST">
        ${formKeyInput(key)}
        <input name="friendRequestId" type="hidden" value="${user.id}" />
        <td class="requestsEntryName">${user.name}</td>
        <td class="requestsEntryStatus">${rel.toString()}</td>
        <td class="requestsEntryButton">
          <input name="friendRequestAccept" class="styledSubmitButton" type="submit" value="Accept" />
          <input name="friendRequestReject" class="styledSubmitButton" type="submit" value="Reject" />
        </td>
      </form>
    </tr>
    """    
  }

  private def requestToHTML(entry: (User, Relationship), kind: Int, key: Int): HTML = {
    requestEntry(entry, if (kind % 2 == 0) 2; else 1, key)
  } 

  private def requestsToHTML(requests: Map[User, Relationship], key: Int): HTML = {
    val sb = new StringBuilder()
    var i = 1
    for (entry <- requests) {
      sb.append(requestToHTML(entry, i, key))
      i = i + 1
    }

    // FIXME: Bedre
    // val rs = requests.zipWithIndex 
    // rs foreach {case (r, i) => sb.append(requestToHTML(r, i, key))}

    // FIXME: Bedst (hvis det virker :P)
    // requests.zipWithIndex map {case (r, i) => requestToHTML(r, i, key)} mkString

    sb.toString()
  }

  def content(request: ViewRequestsPageRequest, key: Int): HTML = {
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
