package dk.itu.ssas.page

import dk.itu.ssas.page.exception._
import dk.itu.ssas.model._

abstract class LoggedInPage extends Page {
  
  def header(title: String, key: Int, user: Option[User]): HTML = {
    user match {
      case None => throw NoUserException()
      case Some(user) =>
        s"""
        <html>
    		 <head>
    		   <meta http-equiv="content-type" content="text/html; charset=UTF-8">
    		   <link rel="stylesheet" type="text/css" href="style.css" />
    		   <title>
    		    $title
    		   </title>
    		 </head>
    		 <body>
    		  <div id="wrapper">
    		    <div id="topBar">
    		      <div id="logoBox">
    		        RaptorDating.com
    		      </div>
    		      <div id="topBarProfileBox">
    		        <button class="styledButton" type="button">${user.name}</button>
    		      </div>
    		      <div id="searchBox">
    		        <form action="/search" method="POST">
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