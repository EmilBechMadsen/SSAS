package dk.itu.ssas.page

object FriendsPage extends WebPage {
  import dk.itu.ssas.page.request._
  import dk.itu.ssas.model._
  import dk.itu.ssas.page.exception._
  import dk.itu.ssas.Settings.baseUrl
  
  type RequestType = NoRequest

  private def friendEntry(entry: (User, Relationship), kind: Int, key: Key): HTML = {
    val user = entry._1
    val rel  = entry._2
    s"""
    <tr class="listEntryRow listEntryColor${kind}">
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
    friends mapi {
      case (entry, i) => friendEntry(entry, if (i % 2 == 0) 1; else 2, key)
    } mkString("\n")
  }

  def content(request: NoRequest, u: Option[User], key: Key): HTML = {
    val user = u.getOrElse(throw NoUserException())
    s"""
      <div id="myFriendsBox">
        <div id="myFriendsCaption">
          My Friends
        </div>
        <div id="myFriendsListBox">
          <table cellspacing="0">
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