package dk.itu.ssas.page

abstract class Page extends WebPage {
  import dk.itu.ssas.Settings.baseUrl

  def footer: HTML = {
    """
       	</div> <!-- close body content -->
       </div> <!-- close wrapper -->
      </body>
     </html>
    """
  }

  def formKeyInput(key: String): HTML = {
  	s"""
  	<input type='hidden' name='formkey' value='$key' />
  	"""
  }
}
