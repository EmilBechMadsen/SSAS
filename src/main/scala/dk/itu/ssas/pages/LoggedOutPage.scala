package dk.itu.ssas.page

abstract class LoggedOutPage extends Page {
  import dk.itu.ssas._
  import dk.itu.ssas.model._
  import dk.itu.ssas.page.exception._

  def header(title: String, key: Key, user: Option[User]): HTML = {
    user match {
      case Some(_) => throw UnexpectedUserException()
      case None => {
        val minPassword = Settings.security.minPassword
        val maxPassword = Settings.security.maxPassword
        s"""
          <html>
            <head>
              <meta http-equiv="content-type" content="text/html; charset=UTF-8">
              <link rel="stylesheet" type="text/css" href="/static/style.css" />
              <script type="text/javascript" src="/static/xregexp-min.js"></script>
              <script type="text/javascript" src="/static/unicode-base.js"></script>
              <script type="text/javascript" src="/static/verimail.js"></script>
              <script type="text/javascript">
                function validateLogin() {
                  var email = document.forms["loginForm"]["loginEmail"].value;
                  var password = document.forms["loginForm"]["loginPassword"].value;
                  var passwordValid = password.length >= $minPassword && password.length <= $maxPassword;
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
                        if (passwordValid) {
                          result = true;
                        } else {
                          alert("Your password must be at least $minPassword characters long");
                          result = false;
                        }
                      }
                      return result;
                  });
                }                
              </script>
              <title>$title</title>
            </head>
            <body>
            <div id="wrapper">
              <div id="topBar">
                <div id="logoBox">
                  RaptorDating.com
                </div>
                <div id="loginBox">
                  <form name="loginForm" action="/login" method="POST" onsubmit="return validateLogin()">
                    ${formKeyInput(key)}
                    Email: <input name="loginEmail" type="text" />
                    Password: <input name="loginPassword" type="password" />
                    <input class="styledSubmitButton" value="Login" type="submit" />
                  </form>
                </div>
              </div>
              <div> <!-- body content -->
        """
      }
    }
  }
}