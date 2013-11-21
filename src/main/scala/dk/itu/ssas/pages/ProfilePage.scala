package dk.itu.ssas.page

object ProfilePage extends WebPage {
  import dk.itu.ssas.page.request._
  import dk.itu.ssas.model._
  import dk.itu.ssas.page.exception._
  import dk.itu.ssas.Settings.baseUrl

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

  private def address(rel: Option[Relationship], user: User, other: User): HTML = {
    val addr =
      other.address match {
        case Some(a) => a.html
        case None    => "Not listed"
      }

    if (user.admin)
      addr
    else 
      rel match {
        case Some(r) => addr
        case None    => "Only available to friends"
      }
  }

  private def hugButton(rel: Option[Relationship], fromUser: User, toUser: User, key: Key): HTML = {
    rel match {
      case Some(r) =>
        s"""
        <div id="hugBox">
          <form action="${baseUrl}/hug/${toUser.id}" method="POST">
            ${formKeyInput(key)}
            <input type="hidden" name="hugFromUserId" value="${fromUser.id}" />
            <input type="submit" class="styledSubmitButton" value="Hug ${toUser.name.html}" />
          </form>
        </div>
        """
      case None => ""
    }
  }

  private def hobbies(other: User): HTML = {
    other.hobbies map { hobby =>
      s"""<li class="hobbiesListItem">${hobby.html}</li>"""
    } mkString("\n")
  }

  private def emailInfo(user: User, other: User): HTML = {
    if (user.admin) {
      s"""
      <div id="emailBox">
        <span class="profileLabel">Email</span><br />
        <span id="emailText">${other.email.html}</span>
      </div>
      """
    } else ""
  }

  def content(request: ProfilePageRequest, u: Option[User], key: Key): HTML = {
    val user = u.getOrElse(throw NoUserException())
    val other = request.other
    val rel = relationship(user, other)
  	s"""
	    <div id="profileWrapper">
        <div id="profileHeader">
          <div id="profileCaption">
            ${other.name.html}
          </div>
          <div id="profileRequestBox">
          <form action="$baseUrl/profile/${other.id}/request" method="POST">
            ${formKeyInput(key)}
            Request <select name="relationship">
              ${requestOptions(user, other)}
            </select>
            <input class="styledSubmitButton" type="submit" value="Do it" />
          </form>
          </div>
        </div>
        <div id="profileBox">
          <div id="profileLeftBox">
            ${emailInfo(user, other)}
            <div id="relationBox">
              <span class="profileLabel">Your relationship</span><br />
              <span id="relationStatus">${relationshipText(rel)}</span>
            </div>
            <div id="addressBox">
              <span class="profileLabel">Address</span><br />
              <div id="addressInfoBox">
                ${address(rel, user, other)}
              </div>
            </div>
            ${hugButton(rel, user, other, key)}
          </div>
          <div id="profileRightBox">
            <div id="hobbiesBox">
              <span class="profileLabel">Hobbies</span>
              <div id="hobbiesListBox">
                <ul id="hobbiesList">
                  ${hobbies(other)}
                </ul>
              </div>
            </div>
          </div>
        </div>
      </div>
  	"""
  }
}