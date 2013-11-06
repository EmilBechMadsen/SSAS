package dk.itu.ssas.page

object SignupPage extends LoggedOutPage {
  import dk.itu.ssas._
  import dk.itu.ssas.page.request._
  import dk.itu.ssas.Settings.{ baseUrl, staticBaseUrl }

  type RequestType = NoRequest

  def content(request: NoRequest, key: Key): HTML = {
    val minPassword = Settings.security.minPassword
    val maxPassword = Settings.security.maxPassword
    val nameRegex = Settings.security.nameWhitelist.replace("\\", "\\\\")
    val minName = Settings.security.minName
    val maxName = Settings.security.maxName
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

        function validateSignup() {
          var email = document.forms["signupForm"]["signupEmail"].value;
          var name = document.forms["signupForm"]["signupName"].value;
          var verimail = new Comfirm.AlphaMail.Verimail();
          var password = document.forms["signupForm"]["signupPassword"].value;
          var confirm = document.forms["signupForm"]["signupPasswordConfirm"].value;
          var passwordValid = password.length >= $minPassword && password.length <= $maxPassword;
          var confirmationValid = password == confirm;
          var result = false;

          var verify = verimail.verify(email, function(status, message, suggestion) {
              if(status < 0){
                  // Incorrect syntax!
                  alertMessage = "The provided email is invalid.";
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
      <div id="signupPageImageBox">
        <img src="$staticBaseUrl/velociraptor.jpg" />
        <p>
          At RaptorDating, we take security seriously. That means we are doing everything to protect you, our valued customer, against attacks from malicious users.
          <br />
          Currently, we provide defenses against the following attacks:
          <ul>
            <li>Packet sniffing</li>
            <li>Replay attacks</li>
            <li>Response splitting</li>
            <li>SQL injection</li>
            <li>XSRF attacks</li>
            <li>XSS attacks</li>
          </ul>
        </p>
      </div>
      <div id="signupBox">
        <form name="signupForm" action="$baseUrl/signup" method="POST" onsubmit="return validateSignup()">
          ${formKeyInput(key)}
          <fieldset>
            <legend>Sign up</legend>
            <table id="signupTable">
              <tr>
                <td class="signupLabel">Email:</td><td><input name="signupEmail" type="text" size="25" /></td>
              </tr>
              <tr>
                <td class="signupLabel">Name:</td><td><input name="signupName" type="text" size="25" /></td>
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