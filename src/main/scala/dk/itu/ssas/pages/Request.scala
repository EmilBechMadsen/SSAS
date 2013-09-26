package dk.itu.ssas.page.request

sealed abstract class Request
case class EditProfilePageRequest() extends Request
case class FriendsPageRequest() extends Request
case class NoRequest() extends Request
case class ProfilePageRequest() extends Request
case class SearchPageRequest() extends Request
case class ViewRequestsPageRequest() extends Request