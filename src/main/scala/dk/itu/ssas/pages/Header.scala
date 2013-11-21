package dk.itu.ssas.page

abstract class Header extends HTMLElement {
  import dk.itu.ssas.model._

  def render(title: String, key: Key, user: Option[User]): HTML
}