package dk.itu.ssas.page

abstract class Page extends WebPage {
  def footer: HTML = {
    """
       </div> <!-- close wrapper -->
      </body>
     </html>
    """
  }
}