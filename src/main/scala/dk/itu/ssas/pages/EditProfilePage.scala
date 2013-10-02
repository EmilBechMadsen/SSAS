package dk.itu.ssas.page

import dk.itu.ssas.model._

object EditProfilePage extends LoggedInPage {
  import dk.itu.ssas.page.request._

  type RequestType = EditProfilePageRequest

  private def address(user: User): HTML = {
    user.address match {
      case Some(a) => a
      case None => ""
    }
  }

  private def hobbyEntry(hobby: String, index: Int, key: Int): HTML = {
    s"""
    <tr>
      <form method="POST">
        ${formKeyInput(key)}
        <td class="hobbiesListItem">$hobby</td>
        <td>
          <input name="profileHobbyRemoveSubmit" type="submit" value="X" class="styledSubmitButton" />
          <input name="hobbyId" type="hidden" value="$index" />
        </td>
      </form>
    </tr>
    """
  }

  private def hobbies(user: User, key: Int): HTML = {
    user.hobbies mapi {
      case (hobby, i) => hobbyEntry(hobby, i, key)
    } mkString("\n")
  }

  def content(request: EditProfilePageRequest, key: Int): HTML = {
    val user = request.user
  	s"""
  	<script type="text/javascript">
      function submitForms() {
        document.getElementById('profileNameForm').submit()
        document.getElementById('profileAddressForm').submit()
        document.getElementById('profilePasswordForm').submit()
      }
    </script>
  	<div id="profileWrapper">
      <div id="profileHeader">
        <div id="profileCaption">
        <form id="profileNameForm" method="POST">
          ${formKeyInput(key)}
          <input id="profileNameInput" name="profileName" type="text" value="${user.name}" />
        </form>
        </div>
        <div id="profileSaveChangesBox">
          <button type="submit" class="styledSubmitButton" form="profileNameForm profileAddressForm profilePasswordForm">Save Changes</button>
        </div>
      </div>
      <div id="profileBox">
        <div id="profileLeftBox">
          <div id="addressBox">
            <span class="profileLabel">Address</span><br />
            <form id="profileAddressForm" method="POST">
              ${formKeyInput(key)}
              <textarea id="profileAddressInput">
                ${address(user)}
              </textarea>
            </form>
          </div>
          <div id="profilePasswordBox">
            <form id="profilePasswordForm" method="POST">
              ${formKeyInput(key)}
              <span class="profileLabel">Current Password</span><br />
              <input class="profileInput" name="profilePasswordInput" type="password" /><br />
              <span class="profileLabel">New Password</span><br />
              <input class="profileInput" name="profilePasswordInput" type="password" /><br />
              <span class="profileLabel">Confirm New Password</span><br />
              <input class="profileInput" name="profilePasswordInput" type="password" />
            </form>
          </div>
        </div>
        <div id="profileRightBox">
          <div id="hobbiesBox">
            <span class="profileLabel">Your Hobbies</span>
            <div id="hobbiesListBox">
              <table id="hobbiesTable">
                ${hobbies(user, key)}
                <tr>
                  <form method="POST">
                    ${formKeyInput(key)}
                    <td class="hobbiesListItem">
                      <input id="profileNewHobbyInput" type="text" value="New hobby" onfocus="this.value='';" />
                    </td>
                    <td>
                      <input name="profileNewHobbySubmit" type="submit" value="+" class="styledSubmitButton" />
                    </td>
                  </form>
                </tr>
              </table>
            </div>
          </div>
        </div>
      </div>
    </div>
  	"""
  }
}