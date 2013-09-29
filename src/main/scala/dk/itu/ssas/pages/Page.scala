package dk.itu.ssas.page

abstract class Page extends WebPage {
  def footer: HTML = {
    """
       	</div> <!-- close body content -->
       </div> <!-- close wrapper -->
      </body>
     </html>
    """
  }

  def formKeyInput(key: Int): HTML = {
  	s"""
  	<input type='hidden' name='formkey' value='$key' />
  	"""
  }
}