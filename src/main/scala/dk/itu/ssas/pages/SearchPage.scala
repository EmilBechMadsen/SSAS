package dk.itu.ssas.page

object SearchPage extends WebPage {
  import dk.itu.ssas.page.request._
  import dk.itu.ssas.model._
  import dk.itu.ssas.page.exception._
  import dk.itu.ssas.Settings.baseUrl

  type RequestType = SearchPageRequest

  private def searchResultEntry(user: User): HTML = {
    s"""
    <tr class="listEntryRow">
      <td class="searchListEntry">
        <a href="$baseUrl/profile/${user.id}">${user.name.html}</a>
      </td>
    </tr>
    """
  }

  private def searchResult(result: List[User]): HTML = {
    result map { u => searchResultEntry(u) } mkString("\n")
  }

  def content(request: SearchPageRequest, u: Option[User], key: Key): HTML = {
    val user = u.getOrElse(throw NoUserException())
    val result = request.result
  	s"""
    <div id="searchResultsBox" class="content">
      <div class="header">
        <div id="caption">
          <h2>Search Results</h2>
        </div>
      </div>
      <div id="contentBody">
        <table cellspacing="0" style="padding-top: 10px; padding-bottom: 10px;">
          ${searchResult(result)}
        </table>
      </div>  
    </div>
  	"""  	
  }
}