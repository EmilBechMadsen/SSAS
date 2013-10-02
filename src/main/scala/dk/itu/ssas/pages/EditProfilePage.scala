package dk.itu.ssas.page

object EditProfilePage extends LoggedInPage {
  import dk.itu.ssas.model._
  import dk.itu.ssas.page.request._
  import dk.itu.ssas._

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
    val nameRegex = Settings.security.nameWhitelist
    val minName = Settings.security.minName
    val maxName = Settings.security.maxName
    val minPassword = Settings.security.minPassword
    val maxPassword = Settings.security.maxPassword
    val addrRegex = Settings.security.addrWhitelist
    val minAddr = Settings.security.minAddr
    val maxAddr = Settings.security.maxAddr
    val hobbyRegex = Settings.security.hobbyWhitelist
    val minHobby = Settings.security.minHobby
    val maxHobby = Settings.security.maxHobby
  	s"""
  	<script type="text/javascript">
      function submitForms() {
        document.getElementById('profileNameForm').submit();
        document.getElementById('profileAddressForm').submit();
        document.getElementById('profilePasswordForm').submit();
      }

      function validateName() {
        var name = document.forms["profileNameForm"]["profileName"].value;
        var pattern = XRegExp("$nameRegex", 'i');
        var validLength = name.length >= $minName && name.length <= $maxName;
        if (XRegExp.test(name, pattern) && validLength) {
          return true;
        } else {
          alert("Your new name is invalid.");
          return false;
        }
        return false;
      }

      function validateAddress() {
        var address = document.forms["profileAddressForm"]["profileAddress"].value;
        var pattern = XRegExp("$addrRegex", 'i');
        var validLength = address.length >= $minAddr && address.length <= $maxAddr;
        if (XRegExp.test(address, pattern) && validLength) {
          return true;
        } else {
          alert("Your new address is invalid.");
          return false;
        }
        return false;
      }

      function validatePassword {
        var password = document.forms["profilePasswordForm"]["profileCurrentPassword"].value;
        var newPassword = document.forms["profilePasswordForm"]["profileNewPassword"].value;
        var confirmNewPassword = document.forms["profilePasswordForm"]["profileNewPasswordConfirm"].value;

        var validCurrentPassword = password.length >= $minPassword && password.length <= $maxPassword;
        if (validCurrentPassword) {
          if (newPassword.length > 0 || confirmNewPassword > 0) {
            if (newPassword == confirmNewPassword) {
              var newPasswordValid = newPassword.length >= $minPassword && newPassword <= $maxPassword;
              if (newPasswordValid) {
                return true;
              } else {
                alert("Your new password is invalid.");
                return false;
              }
            } else {
              alert("Your new password could not be confirmed.");
              return false;
          } else {
            return true;
          }
        } else {
          alert("The password you entered as your current password is not valid. Please try again.");
          return false;
        }
        return false;
      }

      function validateNewHobby() {
        var hobby = document.forms["profileNewHobbyForm"]["profileNewHobby"].value;
        var pattern = XRegExp("$hobbyRegex", 'i');
        var validLength = hobby.length >= $minAddr && hobby.length <= $maxAddr;
        if (XRegExp.test(hobby, pattern) && validLength) {
          return true;
        } else {
          alert("The new hobby you entered is invalid.");
          return false;
        }
        return false;
      }
    </script>
  	<div id="profileWrapper">
      <div id="profileHeader">
        <div id="profileCaption">
        <form name="profileNameForm" id="profileNameForm" method="POST" onsubmit="return validateName()">
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
            <form name="profileAddressForm" id="profileAddressForm" method="POST" onsubmit="return validateAddress()">
              ${formKeyInput(key)}
              <textarea name="profileAddress" id="profileAddressInput">
                ${address(user)}
              </textarea>
            </form>
          </div>
          <div id="profilePasswordBox">
            <form id="profilePasswordForm" method="POST" onsubmit="return validatePassword()">
              ${formKeyInput(key)}
              <span class="profileLabel">Current Password</span><br />
              <input class="profileInput" name="profileCurrentPassword" type="password" /><br />
              <span class="profileLabel">New Password</span><br />
              <input class="profileInput" name="profileNewPassword" type="password" /><br />
              <span class="profileLabel">Confirm New Password</span><br />
              <input class="profileInput" name="profileNewPasswordConfirm" type="password" />
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
                  <form name="profileNewHobbyForm" method="POST" onsubmit="return validateNewHobby()">
                    ${formKeyInput(key)}
                    <td class="hobbiesListItem">
                      <input name="profileNewHobby" id="profileNewHobbyInput" type="text" value="New hobby" onfocus="this.value='';" />
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