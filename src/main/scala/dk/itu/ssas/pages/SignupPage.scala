package dk.itu.ssas.page

import dk.itu.ssas.page.request._

object SignupPage extends LoggedOutPage {
  type RequestType = NoRequest

  def content(request: NoRequest, key: Int): HTML = 
  s"""
    <script type="text/javascript">
      function validate() {
        var password = document.forms["signupForm"]["signupPassword"].value
        var confirm = document.forms["signupForm"]["signupPasswordConfirm"].value
        var result = password == confirm
        if (!result) {
          alert("Your passwords do not match")
        }
        return result
      }
    </script>
    <div id="signupPageImageBox">
      <img src="http://www.eatthedamncake.com/wordpress/wp-content/uploads/2012/02/velociraptor.jpg"/>
    </div>
    <div id="signupBox">
      <form name="signupForm" action="/signup" method="POST" onsubmit="return validate()">
        ${formKeyInput(key)}
        <fieldset>
          <legend>Sign up</legend>
          <table id="signupTable">
            <tr>
              <td class="signupLabel">Email:</td><td><input name="signupEmail" type="text" size="25" /></td>
            </tr>
            <tr>
              <td class="signupLabel">Password:</td><td><input name="signupPassword" type="password" size="25" /></td>
            </tr>
            <tr>
              <td class="signupLabel">Confirm Password:</td><td><input name="signupPasswordConfirm" type="password" size="25" /></td>
            </tr>
            <tr>
              <td></td><td><input class="styledSubmitButton" value="Sign up!" type="submit" /></td>
            </tr>
          </table>
        </fieldset>
      </form>
    </div>
  """
}