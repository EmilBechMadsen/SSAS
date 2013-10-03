package dk.itu.ssas.page

object FriendsPage extends LoggedInPage {
  import dk.itu.ssas.page.request._
  import dk.itu.ssas.model._
  
  type RequestType = FriendsPageRequest

  private def friendEntry(entry: (User, Relationship), kind: Int, key: Key): HTML = {
    val user = entry._1
    val rel  = entry._2
    s"""
    <tr class="listEntryRow listEntryColor${kind}">
      <form actions="/friends" method="POST">
        ${formKeyInput(key)}
        <input name="friendRemoveId" type="hidden" value="${user.id}" />
        <td class="friendEntryName"><a href="/profile/${user.id}">${user.name}</a></td>
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

  def content(request: FriendsPageRequest, key: Key): HTML = {
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
            ${friendsToHTML(request.user.friends, key)}
          </table>
        </div>  
      </div>
    """
  }
}