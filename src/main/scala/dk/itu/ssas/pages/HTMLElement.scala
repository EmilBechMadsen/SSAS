package dk.itu.ssas.page

trait HTMLElement {
  def formKeyInput(key: String): HTML = {
    s"""
    <input type='hidden' name='formkey' value='$key' />
    """
  }
}