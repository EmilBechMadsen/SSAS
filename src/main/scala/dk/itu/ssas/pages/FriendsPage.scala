package dk.itu.ssas.page

object FriendsPage extends WebPage {
  import dk.itu.ssas.page.request._
  import dk.itu.ssas.model._
  import dk.itu.ssas.page.exception._
  import dk.itu.ssas.Settings.baseUrl
  
  type RequestType = NoRequest

  private def friendEntry(entry: (User, Relationship), key: Key): HTML = {
    val user = entry._1
    val rel  = entry._2
    s"""
    <tr class="listEntryRow">
      <form actions="$baseUrl/friends" method="POST">
        ${formKeyInput(key)}
        <input name="friendRemoveId" type="hidden" value="${user.id}" />
        <td class="friendEntryName"><a href="$baseUrl/profile/${user.id}">${user.name.html}</a></td>
        <td class="friendEntryStatus">${rel.prettyPrint}</td>
        <td><input name="friendRemove" class="styledSubmitButton" type="submit" value="Remove" /></td>
      </form>
    </tr>
    """    
  }

  private def friendsToHTML(friends: Map[User, Relationship], key: Key): HTML = {
    friends map { entry => friendEntry(entry, key) } mkString("\n")
  }

  def content(request: NoRequest, u: Option[User], key: Key): HTML = {
    val user = u.getOrElse(throw NoUserException())
    s"""
      <div id="myFriendsBox" class="content">
        <div class="header"
          <div id="caption">
            <h2>My Friends</h2>
          </div>
        <div id="contentBody">
          <table cellspacing="0" style="padding-top: 10px; padding-bottom: 10px;">
            <tr>
              <th class="myFriendsListHeaders">Name</th>
              <th class="myFriendsListHeaders">Status</th>
              <th></th>
            </tr>
            ${friendsToHTML(user.friends, key)}
          </table>
        </div>  
      </div>
    """
  }
}