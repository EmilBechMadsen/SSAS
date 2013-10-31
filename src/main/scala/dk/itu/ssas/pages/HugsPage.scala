package dk.itu.ssas.page

object HugsPage extends LoggedInPage {
  import dk.itu.ssas.page.request._
  import dk.itu.ssas.model._
  import dk.itu.ssas.Settings.baseUrl
  import java.sql.Timestamp
  import java.text.SimpleDateFormat
  import java.sql.Date

  type RequestType = HugsPageRequest

  def formatTime(timestamp: Timestamp): HTML = {
    val date = new Date(timestamp.getTime)
    val pattern = "dd/MM/yyyy, HH:mm"
    val dateFormat = new SimpleDateFormat(pattern)
    dateFormat.format(date)
  }

  def unseenHugTableRow (hug: Hug)(fromUser: User): HTML = {
    s"""
    <tr class="hugsTableRow">
     <td class="hugsTableName"><a href="${baseUrl}/profile/${fromUser.id}" class="hugProfileLink">${fromUser.name}</a> just hugged you!</td>
     <td class="hugsTableDate">${formatTime(hug.time)}</td>
    </tr>
    """
  }

  def seenHugTableRow (hug: Hug)(fromUser: User): HTML = {
    s"""
    <tr class="hugsTableRow">
     <td class="hugsTableName"><a href="${baseUrl}/profile/${fromUser.id}" class="hugProfileLink">${fromUser.name}</a> hugged you</td>
     <td class="hugsTableDate">${formatTime(hug.time)}</td>
    </tr>
    """
  }

  def listHugs(hugs: List[Hug], f: Hug => User => HTML): HTML = {
    hugs.map { hug => 
      hug.fromUser match {
        case Some(u) => f(hug)(u)
        case None => ""
      }
    } mkString("\n")
  }

  def content(request: HugsPageRequest, key: Key): HTML = {
    val user = request.user
    val (unseenHugs, seenHugs) = user.hugs
    s"""
    <script type="text/javascript">
      function hugsSeen() {
        var xmlhttp = new XMLHttpRequest();
        xmlhttp.open("POST", "${baseUrl}/hugs/seen", true);
        xmlhttp.send();
      }
    </script>
    <div id="hugsContent" class="content" onload="hugsSeen();">
      <div class="header">
        <div id="caption">
          <h2>My Hugs</h2>
        </div>
      </div>
      <div id="contentBody">
        <h3 class="hugsCaption">New hugs</h3>
          <table id="hugsTable" cellspacing="0" cellpadding="0">
            ${listHugs(unseenHugs, unseenHugTableRow)}
          </table>
        <h3 class="hugsCaption">Old hugs</h3>
          <table id="hugsTable" cellspacing="0" cellpadding="0">
            ${listHugs(seenHugs, seenHugTableRow)}
          </table>
      </div>
    </div>
    """
  }
}

