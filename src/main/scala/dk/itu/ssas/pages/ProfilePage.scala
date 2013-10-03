package dk.itu.ssas.page

object ProfilePage extends LoggedInPage {
  import dk.itu.ssas.page.request._
  import dk.itu.ssas.model._

  type RequestType = ProfilePageRequest

  private def relationship(user: User, other: User): Option[Relationship] = {
    user.friends find {
      case (u, r) => u == other
    } match {
      case Some((u, r)) => Some(r)
      case None         => None
    }
  }

  private def relationshipOption(rel: Relationship): HTML = {
    s"""<option value="$rel">${rel.prettyPrint}</option>"""
  }

  private def relationshipText(rel: Option[Relationship]): HTML = {
    rel match {
      case Some(r) => r.prettyPrint
      case None    => "None"
    }
  }

  private def requestOptions(user: User, other: User): HTML = {
    val sb = new StringBuilder()
    relationship(user, other) match {
      case Some(rel) =>
        rel match {
          case Friendship =>
            sb.append(relationshipOption(Romance))
            sb.append(relationshipOption(Bromance))
          case Romance =>
            sb.append(relationshipOption(Friendship))
            sb.append(relationshipOption(Bromance))
          case Bromance =>
            sb.append(relationshipOption(Friendship))
            sb.append(relationshipOption(Romance))        
        }
      case None =>
        sb.append(relationshipOption(Friendship))
        sb.append(relationshipOption(Romance))
        sb.append(relationshipOption(Bromance))
    }
    sb.toString()
  }

  private def address(rel: Option[Relationship], other: User): HTML = {
    rel match {
      case Some(r) => other.address match {
        case Some(a) => a
        case None    => "Not listed"
      }
      case None    => "Only available to friends"
    }
  }

  private def hobbies(other: User): HTML = {
    other.hobbies map { hobby =>
      s"""<li class="hobbiesListItem">$hobby</li>"""
    } mkString("\n")
  }

  def content(request: ProfilePageRequest, key: Key): HTML = {
    val rel = relationship(request.user, request.other)
  	s"""
	    <div id="profileWrapper">
        <div id="profileHeader">
          <div id="profileCaption">
            ${request.other.name}
          </div>
          <div id="profileRequestBox">
          <form action="/profile/${request.other.id}/request" method="POST">
            ${formKeyInput(key)}
            Request <select name="relationship">
              ${requestOptions(request.user, request.other)}
            </select>
            <input class="styledSubmitButton" type="submit" value="Do it" />
          </form>
          </div>
        </div>
        <div id="profileBox">
          <div id="profileLeftBox">
          <div id="relationBox">
            <span class="profileLabel">Your relationship</span><br />
            <span id="relationStatus">${relationshipText(rel)}</span>
          </div>
          <div id="addressBox">
            <span class="profileLabel">Address</span><br />
            <div id="addressInfoBox">
              ${address(rel, request.other)}
            </div>
          </div>
          </div>
          <div id="profileRightBox">
            <div id="hobbiesBox">
              <span class="profileLabel">Hobbies</span>
              <div id="hobbiesListBox">
                <ul id="hobbiesList">
                  ${hobbies(request.other)}
                </ul>
              </div>
            </div>
          </div>
        </div>
      </div>
  	"""
  }
}