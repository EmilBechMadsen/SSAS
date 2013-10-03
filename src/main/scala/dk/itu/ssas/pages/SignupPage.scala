package dk.itu.ssas.page

object SignupPage extends LoggedOutPage {
  import dk.itu.ssas._
  import dk.itu.ssas.page.request._

  type RequestType = NoRequest

  def content(request: NoRequest, key: Key): HTML = {
    val minPassword = Settings.security.minPassword
    val maxPassword = Settings.security.maxPassword
    s"""
      <script type="text/javascript">
        function validateSignup() {
          var email = document.forms["signupForm"]["signupEmail"].value
          var verimail = new Comfirm.AlphaMail.Verimail();
          var password = document.forms["signupForm"]["signupPassword"].value
          var confirm = document.forms["signupForm"]["signupPasswordConfirm"].value
          var passwordValid = password.length >= 8 && password.length <= 255
          var confirmationValid = password == confirm
          var result = false;

          var verify = verimail.verify(email, function(status, message, suggestion) {
              if(status < 0){
                  // Incorrect syntax!
                  alertMessage = "The provided email is invalid."
                  if(suggestion) {
                    alertMessage += "\nDid you mean " + suggestion + "?";
                  }
                  alert(alertMessage);
                  result = false;
              } else { // Email is valid
                if (confirmationValid) {
                  if (passwordValid) {
                    result = true;
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