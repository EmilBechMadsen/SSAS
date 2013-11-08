package dk.itu.ssas.page

object AdminPage extends LoggedInPage {
  import dk.itu.ssas.page.HTML
  import dk.itu.ssas.model._
  import dk.itu.ssas.page.request._
  import dk.itu.ssas.page.exception._
  import dk.itu.ssas._
  import dk.itu.ssas.Settings.baseUrl

  type RequestType = AdminPageRequest

  private def promoteInput: HTML = {
    """
    <input name="adminUserPromote" class="styledSubmitButton" type="submit" value="Promote" />
    """
  }

  private def demoteInput: HTML = {
    """
    <input name="adminUserDemote" class="styledSubmitButton" type="submit" value="Demote " />
    """
  }

  private def userToHTML(user: User, key: Key): HTML = {
    val adminStatusInput: HTML = if (user.admin) demoteInput; else promoteInput
    s"""
    <tr class="adminUserEntry">
      <form action="${baseUrl}/admin/toggleAdmin/${user.id}" method="POST">
        ${formKeyInput(key)}
        <input type="hidden" name="adminUserId" value="${user.id}" />
        <td class="adminUserEntryName">
          <a href="${baseUrl}/profile/${user.id}">${user.name.html}</a>
        </td>
        <td class="adminUserEntryAdminButton adminUserEntryButton">
          $adminStatusInput
        </td>
      </form>
      <form action="${baseUrl}/admin/delete/${user.id}" method="POST">
        ${formKeyInput(key)}
        <td class="adminUserEntryRemoveButton">
          <input name="adminUserRemove" class="styledSubmitButton" type="submit" value="Remove" />
        </td>
      </form>
    </tr>
    """
  }

  private def users(userList: List[User], key: Key): HTML = {
    userList map { u => userToHTML(u, key) } mkString("\n")
  }

  def content(request: AdminPageRequest, key: Key): HTML = {
    val user = request.user
    user.admin match {
      case false => throw new NotAdminException()
      case true  =>
        val nameRegex = Settings.security.nameWhitelist
        val minName = Settings.security.minName
        val maxName = Settings.security.maxName
        val minPassword = Settings.security.minPassword
        val maxPassword = Settings.security.maxPassword

        s"""
          <script type="text/javascript">
            function validateName(name) {
              var pattern = XRegExp("$nameRegex", 'i');
              var validLength = name.length >= $minName && name.length <= $maxName;
              if (XRegExp.test(name, pattern) && validLength) {
                return true;
              } else {
                alert("Your name for the new user is invalid.");
                return false;
              }
              return false;
            }

            function validateAddUser() {
              var email = document.forms["adminAddUserForm"]["signupEmail"].value;
              var name  = document.forms["adminAddUserForm"]["signupName"].value;
              var password = document.forms["adminAddUserForm"]["signupPassword"].value;
              var confirm = document.forms["adminAddUserForm"]["signupPasswordConfirm"].value;

              var passwordValid = password.length >= $minPassword && password.length <= $maxPassword
              var confirmationValid = password == confirm
              var result = false;

              var verify = verimail.verify(email, function(status, message, suggestion) {
                  if(status < 0){
                      // Incorrect syntax!
                      alertMessage = "The provided email is invalid."
                      if(suggestion) {
                        alertMessage += "Did you mean " + suggestion + "?";
                      }
                      alert(alertMessage);
                      result = false;
                  } else { // Email is valid
                    if (confirmationValid) {
                      if (passwordValid) {
                        if (validateName(name)) {
                          result = true;
                        } else {
                          result = false;
                        }
                      } else {
                        alert("Your password must be at least $minPassword characters long");
                        result = false;
                      }
                    } else {
                      alert("Your passwords do not match")
                      result = false;
                    }
                  }
              });
              return result;
            }
          </script>
          <div id="adminWrapper">
            <div id="adminHeader">
              <div id="adminCaption">
                Admin page
              </div>
            </div>
            <div id="adminBox">
              <div id="adminAddUserBox">
                <fieldset id="adminAddUserFieldset">
                  <legend>Add User</legend>
                  <form name="adminAddUserForm" action="$baseUrl/admin/createUser" method="POST" onsubmit="return validateAddUser()" />
                    ${formKeyInput(key)}
                    <div id="adminAddUserFieldsetContent">
                      <table cellspacing="0" cellpadding="0">
                        <tr>
                          <td>
                            <div class="adminAddUserLabel">Email:</div>
                          </td>
                          <td><input name="signupEmail" type="text" size="25" /></td>
                          <td>
                            <div class="adminAddUserLabel">Password:</div>
                          </td>
                          <td><input name="signupPassword" type="password" size="25" /></td>
                        </tr>
                        <tr>
                          <td>
                            <div class="adminAddUserLabel">Name:</div>
                          </td>
                          <td><input name="signupName" type="text" size="25" /></td>
                          <td>
                            <div class="adminAddUserLabel">Confirm Password:</div>
                          </td>
                          <td><input name="signupPasswordConfirm" type="password" size="25" /></td>
                        </tr>
                        <tr>
                          <td></td>
                          <td></td>
                          <td></td>
                          <td><input name="adminAddUserSubmit" class="styledSubmitButton" type="submit" value="Add" /></td>
                        </tr>
                      </table>
                    </div>
                  </form>
                </fieldset>
              </div>
              <div id="adminUsersBox">
                <h2>Users</h2>
                <table cellspacing="0" cellpadding="0" id="adminUsersTable">
                  ${users(User.all, key)}
                </table>     
              </div>
            </div>
          </div>
        """
    }
  }
}