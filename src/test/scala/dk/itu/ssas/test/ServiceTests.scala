package dk.itu.ssas.test
	
import spray.testkit.ScalatestRouteTest
import spray.http._
import org.scalatest.FunSuite
import spray.routing.HttpService
import akka.testkit.TestActorRef
import dk.itu.ssas._

class ServiceTests extends FunSuite with ScalatestRouteTest with HttpService {
	val actorRef = TestActorRef[Service]
	val actor = actorRef.underlyingActor

  val route = actor.route

	test("Admin rejects unauthorized access attempts") {
		Get("/admin") ~> route ~> check { 
			assert(response.status === StatusCodes.InternalServerError)
		}
	}
}
