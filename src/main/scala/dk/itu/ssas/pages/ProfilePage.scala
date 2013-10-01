package dk.itu.ssas.page

  import dk.itu.ssas.db._

object ProfilePage extends LoggedInPage {
  import dk.itu.ssas.page.request._
  import dk.itu.ssas.model._

  type RequestType = ProfilePageRequest

  private def isFriend(user: User, other: User): Option[Relationship] = {
    var result: Option[Relationship] = None
    for ((user, rel) <- user.friends) {
      if (user == other)
        result = Some(rel)
    }
    result
  }

  private def relationshipOption(rel: Relationship): HTML = {
    val asString = rel.toString()
    s"""<option value="$asString">$asString</option>"""
  }

  private def relationshipText(rel: Option[Relationship]): HTML = {
    rel match {
      case Some(r) => r.toString()
      case None    => "None"
    }
  }

  private def requestOptions(user: User, other: User): HTML = {
    val sb = new StringBuilder()
    isFriend(user, other) match {
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
        case None => "Not listed"
      }
      case None => "Only available to friends"
    }
  }

  private def hobbies(other: User): HTML = {
    other.hobbies.foldLeft(new StringBuilder())((sb, h) => {
        val hobby = s"""
          <li class="hobbiesListItem">${h}</li>
        """
        sb.append(hobby)
      }).toString()
  }

  def content(request: ProfilePageRequest, key: Int): HTML = {
    val rel = isFriend(request.user, request.other)
  	s"""
	    <div id="profileWrapper">
        <div id="profileHeader">
          <div id="profileCaption">
            ${request.user.name}
          </div>
          <div id="profileRequestBox">
          <form method="POST">
            ${formKeyInput(key)}
            Request <select>
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
                  ${hobbies{request.other}}
                </ul>
              </div>
            </div>
          </div>
        </div>
      </div>
  	"""
  }
}