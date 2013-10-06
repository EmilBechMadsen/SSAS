package dk.itu.ssas.page

object SearchPage extends LoggedInPage {
  import dk.itu.ssas.page.request._
  import dk.itu.ssas.model._
  import dk.itu.ssas.Settings.baseUrl

  type RequestType = SearchPageRequest

  private def searchResultEntry(user: User, kind: Int): HTML = {
    s"""
    <tr class="listEntryRow listEntryColor${kind}">
      <td class="searchListEntry">
        <a href="$baseUrl/profile/${user.id}">${user.name}</a>
      </td>
    </tr>
    """
  }

  private def searchResult(result: List[User]): HTML = {
    result mapi { 
      case (u, i) => searchResultEntry(u, if (i % 2 == 0) 1; else 2) 
    } mkString("\n")
  }

  def content(request: SearchPageRequest, key: Key): HTML = {
	s"""
  <div id="searchResultsBox">
    <div id="searchResultsCaption">
      Search Results
    </div>
    <div id="searchResultsListBox">
      <table cellspacing="0" style="width: 100%;">
        ${searchResult(request.result)}
      </table>
    </div>  
  </div>
	"""  	
  }
}