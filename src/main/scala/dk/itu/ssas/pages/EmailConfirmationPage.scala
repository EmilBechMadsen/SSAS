package dk.itu.ssas.page

import dk.itu.ssas.page.request._

object EmailConfirmationPage extends LoggedOutPage {
  type RequestType = NoRequest
  
  def content(request: NoRequest, key: Int): HTML = {
    s"""
    <div id="confirmationWrapper">
      <fieldset>
        <legend>Confirm Your Email</legend>
        <div id="confirmationContent">
          <span>To confirm your email, please enter your password</span>
          <form method="POST">
            ${formKeyInput(key)}
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