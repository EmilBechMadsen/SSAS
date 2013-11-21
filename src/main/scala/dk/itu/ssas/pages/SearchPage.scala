package dk.itu.ssas.page

object SearchPage extends WebPage {
  import dk.itu.ssas.page.request._
  import dk.itu.ssas.model._
  import dk.itu.ssas.page.exception._
  import dk.itu.ssas.Settings.baseUrl

  type RequestType = SearchPageRequest

  private def searchResultEntry(user: User, kind: Int): HTML = {
    s"""
    <tr class="listEntryRow listEntryColor${kind}">
      <td class="searchListEntry">
        <a href="$baseUrl/profile/${user.id}">${user.name.html}</a>
      </td>
    </tr>
    """
  }

  private def searchResult(result: List[User]): HTML = {
    result mapi { 
      case (u, i) => searchResultEntry(u, if (i % 2 == 0) 1; else 2) 
    } mkString("\n")
  }

  def content(request: SearchPageRequest, u: Option[User], key: Key): HTML = {
    val user = u.getOrElse(throw NoUserException())
    val result = request.result
  	s"""
    <div id="searchResultsBox">
      <div id="searchResultsCaption">
        Search Results
      </div>
      <div id="searchResultsListBox">
        <table cellspacing="0" style="width: 100%;">
          ${searchResult(result)}
        </table>
      </div>  
    </div>
  	"""  	
  }
}