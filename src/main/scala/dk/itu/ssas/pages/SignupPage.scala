package dk.itu.ssas.page

object SignupPage extends LoggedOutPage {
  import dk.itu.ssas._
  import dk.itu.ssas.page.request._

  type RequestType = NoRequest

  def content(request: NoRequest, key: Int): HTML = {
    val minPassword = Settings.security.minPassword
    val maxPassword = Settings.security.maxPassword
    val emailRegex = """^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,4}$"""
    s"""
      <script type="text/javascript">
        function validateSignup() {
          var email = document.forms["signupForm"]["signupEmail"].value
          var emailPattern = /$emailRegex/i;
          var emailValid = input.test(pattern)

          var password = document.forms["signupForm"]["signupPassword"].value
          var confirm = document.forms["signupForm"]["signupPasswordConfirm"].value
          var passwordValid = password.length >= $minPassword && password.length <= $maxPassword
          var confirmationValid = password == confirm

          if (confirmationValid) {
            if (emailValid) {
              if (passwordValid) {
                return true
              } else {
                alert("Your password must be at least $minPassword characters long")
              }
            } else {
              alert("The provided email is invalid")
            } 
          else {
            alert("Your passwords do not match")
          }
          return false
        }
      </script>
      <div id="signupPageImageBox">
        <img src="http://www.eatthedamncake.com/wordpress/wp-content/uploads/2012/02/velociraptor.jpg"/>
      </div>
      <div id="signupBox">
        <form name="signupForm" action="/signup" method="POST" onsubmit="return validateSignup()">
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
}