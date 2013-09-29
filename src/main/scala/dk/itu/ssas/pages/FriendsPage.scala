package dk.itu.ssas.page

import dk.itu.ssas.page.request._
import dk.itu.ssas.model._

object FriendsPage extends LoggedInPage {
  type RequestType = FriendsPageRequest

  private def friendEntry(entry: (Relationship, User), kind: Int, key: Int): HTML = {
    val rel = entry._1
    val user = entry._2
    s"""
    <tr class="listEntryRow listEntryColor${kind}">
      <form method="POST">
        ${formKeyInput(key)}
        <input type="hidden" value="${user.id}" />
        <td class="friendEntryName"><a href="">${user.name}</a></td>
        <td class="friendEntryStatus">${rel.toString()}</td>
        <td><input name="friendRemove" class="styledSubmitButton" type="submit" value="Remove" /></td>
      </form>
    </tr>
    """    
  }

  private def friendToHTML(entry: (Relationship, User), kind: Int, key: Int): HTML = {
    friendEntry(entry, if (kind % 2 == 0) 2; else 1, key)
  } 

  private def friendsToHTML(friends: Map[Relationship, User], key: Int): HTML = {
    val sb = new StringBuilder()
    var i = 1
    for (entry <- friends) {
      sb.append(friendToHTML(entry, i, key))
      i = i + 1
    }
    sb.toString()
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