package dk.itu.ssas.page

object EmailConfirmationPage extends WebPage {
  import dk.itu.ssas._
  import dk.itu.ssas.model._
  import dk.itu.ssas.page.request._

  type RequestType = EmailConfirmationPageRequest
  
  def content(request: EmailConfirmationPageRequest, user: Option[User], key: Key): HTML = {
    val minPassword = Settings.security.minPassword
    val maxPassword = Settings.security.maxPassword
    val token = request.token

    s"""
    <script type="text/javascript">
      function validatePassword() {
        var password = document.forms["emailConfirmationPasswordForm"]["emailConfirmationPassword"].value;
        var passwordValid = password.length >= $minPassword && password.length <= $maxPassword;
        if (passwordValid) {
          return true;
        } else {
          alert("The password you entered is invalid.")
          return false;
        }
        return false;
      } 
    </script>
    <div id="confirmationWrapper">
      <fieldset>
        <legend>Confirm Your Email</legend>
        <div id="confirmationContent">
          <span>To confirm your email, please enter your password</span>
          <form name="emailConfirmationPasswordForm" method="POST" onsubmit="return validatePassword()">
            ${formKeyInput(key)}
            <input name="token" type="hidden" value="$token" />
            <div id="confirmationFormContent">
              <input name="emailConfirmationPassword" id="confirmationInput" type="password" />
              <input name="emailConfirmationSubmit" class="styledSubmitButton" type="submit" value="Confirm Email" />
            </div>
          </form>
        </div>
      </fieldset>
    </div>
    """
  }
}