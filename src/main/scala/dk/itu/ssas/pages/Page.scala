package dk.itu.ssas.page

abstract class Page extends WebPage {
  def footer: HTML = {
    "COPYRIGHT DINMOR"
  }
}