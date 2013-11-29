package dk.itu.ssas.page

object SearchPage extends WebPage {
  import dk.itu.ssas.page.request._
  import dk.itu.ssas.model._
  import dk.itu.ssas.page.exception._
  import dk.itu.ssas.Settings.baseUrl

  type RequestType = SearchPageRequest

  private def searchResultEntry(url: String, name: String): HTML = {
    s"""
    <tr class="listEntryRow">
      <td class="searchListEntry">
        <a href="$url">$name</a>
      </td>
    </tr>
    """
  }

  private def searchResult(result: List[User]): HTML = {
    result map { u => searchResultEntry(s"$baseUrl/profile/${u.id}", u.name.html) } mkString("\n")
  }

  private def remoteSearchResult(remoteResult: Map[String, List[RemoteUser]]): HTML = {
   (for ((name, result) <- remoteResult) yield if(result.length > 0) {
      s"""
      <h3>${name.html}</h3>
      <table cellspacing="0" style="padding-top: 10px; padding-bottom: 10px;">
          ${result map { u => searchResultEntry(u.url.html, u.name.html) } mkString("\n")}
      </table>"""
   }).mkString("\n")
  }

  def content(request: SearchPageRequest, u: Option[User], key: Key): HTML = {
    val user = u.getOrElse(throw NoUserException())
    val result = request.local
    val remoteResult = request.remote

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
        ${remoteSearchResult(remoteResult)}
      </div>  
    </div>
  	"""  	
  }
}