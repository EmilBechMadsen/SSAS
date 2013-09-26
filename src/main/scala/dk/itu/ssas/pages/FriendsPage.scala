package dk.itu.ssas.page

import dk.itu.ssas.page.request._

object FriendsPage extends LoggedInPage {
  type RequestType = FriendsPageRequest

  def content(request: FriendsPageRequest): HTML = {
    s"""
      <div> <!-- body content -->
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
              <tr class="friendEntryRow">
                <td class="friendEntryName"><a href="">Heinrich Luitpold Himmler</a></td><td class="friendEntryStatus">Friendship</td><td><button class="styledButton" type="button">Remove</button></td>
              </tr>
            </table>
          </div>  
        </div>
      </div>
    """
  }
}