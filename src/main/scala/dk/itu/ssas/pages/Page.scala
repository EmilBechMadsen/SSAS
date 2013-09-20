package dk.itu.ssas.page

trait Page {
	def topBar: TopBar

	def content: Page
}