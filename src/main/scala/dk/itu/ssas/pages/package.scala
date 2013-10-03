package dk.itu.ssas.page

object `package` {
  type HTML = String
  type Key = String

  implicit class IterableExtensions[A](iter: Iterable[A]) {
  	def mapi[B](f: ((A, Int)) => B): Iterable[B] = iter.zipWithIndex map f
  }
}