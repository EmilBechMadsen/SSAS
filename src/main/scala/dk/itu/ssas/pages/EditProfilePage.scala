package dk.itu.ssas.page  

object EditProfilePage extends WebPage {
  import dk.itu.ssas.model._
  import dk.itu.ssas.page.request._
  import dk.itu.ssas.page.exception._
  import dk.itu.ssas._
  import dk.itu.ssas.Settings.baseUrl

  type RequestType = NoRequest

  private def address(user: User): HTML = {
    user.address match {
      case Some(a) => a.html
      case None => ""
    }
  }

  private def hobbyEntry(hobby: String, user: User, index: Int, key: Key): HTML = {
    s"""
    <tr>
      <form action="$baseUrl/profile/${user.id}/edit/hobby/remove" method="POST">
        ${formKeyInput(key)}
        <td class="hobbiesListItem">$hobby</td>
        <td>
          <input name="profileHobbyRemoveSubmit" type="submit" value="X" class="styledSubmitButton" />
          <input name="profileHobby" type="hidden" value="$hobby" />
        </td>
      </form>
    </tr>
    """
  }

  private def hobbies(user: User, key: Key): HTML = {
    user.hobbies mapi {
      case (hobby, i) => hobbyEntry(hobby.html, user, i, key)
    } mkString("\n")
  }

  def content(request: NoRequest, u: Option[User], key: Key): HTML = {
    val user = u.getOrElse(throw NoUserException())
    val nameRegex = Settings.security.nameWhitelist.replace("\\", "\\\\")
    val minName = Settings.security.minName
    val maxName = Settings.security.maxName
    val minPassword = Settings.security.minPassword
    val maxPassword = Settings.security.maxPassword
    val addrRegex = Settings.security.addrWhitelist.replace("\\", "\\\\")
    val minAddr = Settings.security.minAddr
    val maxAddr = Settings.security.maxAddr
    val hobbyRegex = Settings.security.hobbyWhitelist.replace("\\", "\\\\")
    val minHobby = Settings.security.minHobby
    val maxHobby = Settings.security.maxHobby
    val maxHobbies = Settings.security.maxHobbies
    s"""
    <script type="text/javascript">
      function validateName() {
        var name = document.forms["profileForm"]["profileName"].value;
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
        var address = document.forms["profileForm"]["profileAddress"].value;
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

      function validatePassword() {
        var password = document.forms["profileForm"]["profileCurrentPassword"].value;
        var newPassword = document.forms["profileForm"]["profileNewPassword"].value;
        var confirmNewPassword = document.forms["profileForm"]["profileNewPasswordConfirm"].value;

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
            }
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
        var hobbyCount = document.querySelectorAll(".hobbiesListItem").length;
        if (hobbyCount >= $maxHobbies) {
          alert("You have too many hobbies!");
          return false;
        }

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

      function validate() {
        validateName() && validateAddress() && validatePassword()
      }
    </script>
    <div id="profileWrapper">
      <div id="profileHeader">
        <div id="profileCaption">
          <h1>Edit profile</h1>
        </div>
      </div>
      <div id="profileBox">
        <div id="profileLeftBox">
          <form action="$baseUrl/profile/${user.id}/edit/info" name="profileForm" id="profileNameForm" method="POST" onsubmit="return validate()">
            ${formKeyInput(key)}
            <input id="profileNameInput" name="profileName" type="text" value="${user.name.html}" />
            <div id="addressBox">
              <span class="profileLabel">Address</span><br />
                <textarea name="profileAddress" id="profileAddressInput">${address(user)}</textarea>
            </div>
            <div id="profilePasswordBox">
                <span class="profileLabel">Current Password</span><br />
                <input class="profileInput" name="profileCurrentPassword" type="password" /><br />
                <span class="profileLabel">New Password</span><br />
                <input class="profileInput" name="profileNewPassword" type="password" /><br />
                <span class="profileLabel">Confirm New Password</span><br />
                <input class="profileInput" name="profileNewPasswordConfirm" type="password" />
            </div>
            <input type="submit" class="styledSubmitButton" value="Save Changes" />
          </form>
        </div>
        <div id="profileRightBox">
          <div id="hobbiesBox">
            <span class="profileLabel">Your Hobbies</span>
            <div id="hobbiesListBox">
              <table id="hobbiesTable">
                ${hobbies(user, key)}
                <tr>
                  <form action="$baseUrl/profile/${user.id}/edit/hobby/add" name="profileNewHobbyForm" method="POST" onsubmit="return validateNewHobby()">
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