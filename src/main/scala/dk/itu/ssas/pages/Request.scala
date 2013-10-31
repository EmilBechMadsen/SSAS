package dk.itu.ssas.page.request

import dk.itu.ssas.model._
import java.util.UUID

sealed abstract class Request
case class AdminPageRequest(user: User) extends Request
case class EditProfilePageRequest(user: User) extends Request
case class EmailConfirmationPageRequest(token: UUID) extends Request
case class FriendsPageRequest(user: User) extends Request
case class NoRequest() extends Request
case class ProfilePageRequest(user: User, other: User) extends Request
case class SearchPageRequest(result: List[User]) extends Request
case class ViewRequestsPageRequest(user: User) extends Request
case class HugsPageRequest(user: User) extends Request
