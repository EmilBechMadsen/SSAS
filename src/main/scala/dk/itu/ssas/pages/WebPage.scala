package dk.itu.ssas.page

trait WebPage extends HTMLElement {
  import dk.itu.ssas._
  import dk.itu.ssas.page.request._
  import dk.itu.ssas.model._
  import dk.itu.ssas.page.exception._
  import dk.itu.ssas.Settings.{ baseUrl, staticBaseUrl }

  type RequestType <: Request

  protected def content(request: RequestType, user: Option[User], key: Key): HTML

  protected def footer: HTML = {
    """
        </div> <!-- close body content -->
       </div> <!-- close wrapper -->
      </body>
     </html>
    """
  }

  def render(title: String, key: Key, user: Option[User], request: RequestType): HTML = {
  	val page = new StringBuilder()
  	val head =
      user match {
        case Some(u) =>
          if (u.isLoggedIn) {
            LoggedInHeader(title, key, user)
          } else {
            LoggedOutHeader(title, key, user)
          }
        case None => LoggedOutHeader(title, key, user)
      }
  	val body = content(request, user, key)
  	page.append(head).append(body).append(footer).toString()
  }
}
