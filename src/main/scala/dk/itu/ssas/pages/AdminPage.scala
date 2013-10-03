package dk.itu.ssas.page

object AdminPage extends LoggedInPage {
  import dk.itu.ssas.model._
  import dk.itu.ssas.page.request._
  import dk.itu.ssas.page.exception._

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

  private def userToHTML(user: User): HTML = {
    val adminStatusInput: HTML = if (user.admin) demoteInput; else promoteInput

    s"""
    <tr class="adminUserEntry">
      <form method="POST">
        <input type="hidden" name="adminUserId" value="${user.id}" />
        <td class="adminUserEntryName">
          ${user.name}
        </td>
        <td class="adminUserEntryAdminButton adminUserEntryButton">
          $adminStatusInput
        </td>
        <td class="adminUserEntryRemoveButton">
          <input name="adminUserRemove" class="styledSubmitButton" type="submit" value="Remove" />
        </td>
      </form>
    </tr>
    """
  }

  private def users(userList: List[User]): HTML = {
    userList map userToHTML mkString("\n")
  }

  def content(request: AdminPageRequest, key: Key): HTML = {
    val user = request.user
    user.admin match {
      case false => throw new NotAdminException()
      case true  =>
        s"""
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
                  <form method="POST" onsubmit="return false" />
                  <div id="adminAddUserFieldsetContent">
                    <div class="adminAddUserFieldBox">
                      <span class="adminAddUserLabel">Email:</span>
                      <input name="adminAddUserName" type="text" size="20" />
                    </div>
                    <div class="adminAddUserFieldBox">
                      <span class="adminAddUserLabel">Password:</span>
                      <input name="adminAddUserPassword" type="password" size="20" />
                    </div>
                    <div class="adminAddUserFieldBox">
                      <span class="adminAddUserLabel">Confirm Password:</span>
                      <input name="adminAddUserPasswordConfirm" type="password" size="20" />
                    </div>
                    <div id="adminAddUserSubmitBox">
                      <input name="adminAddUserSubmit" class="styledSubmitButton" type="submit" value="Create" />
                    </div>
                  </div>
                </fieldset>
              </div>
              <div id="adminUsersBox">
                <h2>Users</h2>
                <table cellspacing="0" cellpadding="0" id="adminUsersTable">
                  ${users(User.all)}
                </table>     
              </div>
            </div>
          </div>
        """
    }
  }
}