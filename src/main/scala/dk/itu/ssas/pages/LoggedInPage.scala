package dk.itu.ssas.page

abstract class LoggedInPage extends Page {
  import dk.itu.ssas.page.exception._
  import dk.itu.ssas.model._
  import dk.itu.ssas._
  
  def header(title: String, key: Key, user: Option[User]): HTML = {
    user match {
      case None => throw NoUserException()
      case Some(user) =>
        val searchRegex = Settings.security.nameWhitelist
        val minName = Settings.security.minName
        val maxName = Settings.security.maxName
        s"""
        <html>
          <head>
            <meta http-equiv="content-type" content="text/html; charset=UTF-8">
            <link rel="stylesheet" type="text/css" href="/static/style.css" />
            <script type="text/javascript" src="/static/xregexp-min.js"></script>
            <script type="text/javascript" src="/static/unicode-base.js"></script>
            <script type="text/javascript" src="/static/verimail.js"></script>
            <script type="text/javascript">
              function validateSearch() {
                var input = document.forms["searchForm"]["searchTerm"].value;
                var pattern = XRegExp("$searchRegex", 'i');
                var validLength = input.length >= $minName && input.length <= maxName
                if (XRegExp.test(input, pattern) && validLength) {
                  return true;
                } else {
                  alert("Your search term does not constitute a valid name.")
                  return false;
                }
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
              <div id="topBarProfileBox">
                <a href="/profile/${user.id}">
                  <button class="styledButton" type="button">${user.name}</button>
                </a>
              </div>
              <div id="searchBox">
                <form name="searchForm" action="/search" method="POST" onsubmit="return validateSearch()">
                  ${formKeyInput(key)}
                  <input name="searchTerm" type="text" />
                  <input class="styledSubmitButton" value="Search" type="submit" /> 
                </form>
              </div>
              <div id="logoutBox">
                <form action="/logout" method="POST">
                  ${formKeyInput(key)}
                  <input class="styledSubmitButton" value="Logout" type="submit" />
                </form>
              </div>
            </div>
            <div> <!-- body content -->
        """
    }
  }
}