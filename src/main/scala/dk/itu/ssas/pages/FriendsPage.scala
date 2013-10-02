package dk.itu.ssas.page

import dk.itu.ssas.page.request._
import dk.itu.ssas.model._

object FriendsPage extends LoggedInPage {
  type RequestType = FriendsPageRequest

  private def friendEntry(entry: (User, Relationship), kind: Int, key: Int): HTML = {
    val user = entry._1
    val rel  = entry._2
    s"""
    <tr class="listEntryRow listEntryColor${kind}">
      <form method="POST">
        ${formKeyInput(key)}
        <input type="hidden" value="${user.id}" />
        <td class="friendEntryName"><a href="">${user.name}</a></td>
        <td class="friendEntryStatus">$rel</td>
        <td><input name="friendRemove" class="styledSubmitButton" type="submit" value="Remove" /></td>
      </form>
    </tr>
    """    
  }

  private def friendsToHTML(friends: Map[User, Relationship], key: Int): HTML = {
    friends mapi {
      case (entry, i) => friendEntry(entry, if (i % 2 == 0) 1; else 2, key)
    } mkString("\n")
  }

  def content(request: FriendsPageRequest, key: Int): HTML = {
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