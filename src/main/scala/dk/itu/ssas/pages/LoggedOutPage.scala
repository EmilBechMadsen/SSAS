package dk.itu.ssas.page

import dk.itu.ssas.model._
import dk.itu.ssas.page.exception._

abstract class LoggedOutPage extends Page {

  def header(title: String, key: Int, user: Option[User]): HTML = {
    user match {
      case Some(_) => throw UnexpectedUserException()
      case None =>
        s"""
          <html>
           <head>
             <meta http-equiv="content-type" content="text/html; charset=UTF-8">
             <link rel="stylesheet" type="text/css" href="style.css" />
             <title>$title</title>
           </head>
           <body>
            <div id="wrapper">
              <div id="topBar">
                <div id="logoBox">
                  RaptorDating.com
                </div>
                <div id="loginBox">
                  <form action="/login" method="POST">
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