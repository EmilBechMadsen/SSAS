package dk.itu.ssas.test
	
import akka.testkit.TestActorRef
import dk.itu.ssas._
import dk.itu.ssas.db.DbAccess
import dk.itu.ssas.model._
import java.net.URLEncoder
import org.scalatest.BeforeAndAfter
import org.scalatest.FunSuite
import scala.language.postfixOps
import scala.slick.driver.MySQLDriver.simple._
import scala.slick.driver.MySQLDriver.simple.Database.threadLocalSession
import spray.http._
import spray.routing._
import spray.routing.AuthenticationFailedRejection
import spray.routing.HttpService
import spray.testkit.ScalatestRouteTest

class ServiceTests extends FunSuite 
                      with ScalatestRouteTest 
                      with HttpService 
                      with DatabaseTests 
                      with TestData 
                      with BeforeAndAfter
                      with DbAccess {
	val actorRef = TestActorRef[Service]
	val actor = actorRef.underlyingActor
	val actorRefFactory = system
  val route = actor.route
  var user: User = _
  var password: String = _

  def bodyWithFormKey(body: String) : String = {
    val s = Session()
    "formkey=" + s.newFormKey + body
  }

  before {
    val (tUser, tPassword) = randomLoggedInUser(true, true)
    user = tUser
    password = tPassword
  }

  test("User can sign up and confirm") {
    val name  = randomName
    val email = randomEmail
    val pass  = randomPassword
    val addr  = randomAddress
    val s = Session()

    val entityBody = bodyWithFormKey("&signupEmail=" + email + "&signupName=" + URLEncoder.encode(name, "UTF-8") + "&signupPassword=" + URLEncoder.encode(pass, "UTF-8") + "&signupPasswordConfirm=" + URLEncoder.encode(pass, "UTF-8")) 
    val entity = HttpEntity(ContentType(MediaTypes.`application/x-www-form-urlencoded`, HttpCharsets.`UTF-8`), entityBody)
    
    val post = Post("/signup", entity) ~> addHeader("Cookie", s"ssas_session=$s.key")
    
    post ~> route ~> check {
      assert(response.status === StatusCodes.OK)
    }

    val confirmEntityBody = bodyWithFormKey("&emailConfirmationPassword=" + URLEncoder.encode(pass, "UTF-8"))
    val confirmEntity = HttpEntity(ContentType(MediaTypes.`application/x-www-form-urlencoded`, HttpCharsets.`UTF-8`), confirmEntityBody)
    

    Db.withSession {
        val user = (for {
          u <- Users if (u.email === email)
        } yield u) firstOption

        user match {
          case Some(user) =>
            assert(!user.isConfirmed)
            (for {
              e <- EmailConfirmations if (e.userId === user.id)
            } yield e.guid) firstOption match {
              case Some(g) => {
                val confirmPost = Post("/confirm/" + g, confirmEntity) ~> addHeader("Cookie", "ssas_session=" + s.key)
                
                confirmPost ~> route ~> check {
                  assert(response.status === StatusCodes.SeeOther)
                  assert(user.isConfirmed)
                }
              }
              case None => assert(false)
            }  
          case None => assert(false)
        }
      }
  }

  test("Login works") {
  		val (user, password) = randomUser(true, true)
  		val s = Session()
  		val entityBody = bodyWithFormKey("&loginEmail=" + user.email + "&loginPassword=" + URLEncoder.encode(password, "UTF-8")) 
  		val entity = HttpEntity(ContentType(MediaTypes.`application/x-www-form-urlencoded`, HttpCharsets.`UTF-8`), entityBody)
  		val post = Post("/login", entity) ~> addHeader("Cookie", s"ssas_session=$s.key")
  		post ~> route ~> check {
  			assert(response.status === StatusCodes.SeeOther)
        assert(user.isLoggedIn)
  		}
  }

  test("User can request friendship, and user can accept it") {
    val (otherUser, otherPassword) = randomLoggedInUser(true, true)

    val entityBody = bodyWithFormKey("&relationship=Friendship")
    val entity = HttpEntity(ContentType(MediaTypes.`application/x-www-form-urlencoded`, HttpCharsets.`UTF-8`), entityBody)

    user.session match {
      case Some(session) => {
        val post = Post("/profile/" + otherUser.id + "/request", entity) ~> addHeader("Cookie", s"ssas_session=$session")
        post ~> route ~> check {
          assert(response.status === StatusCodes.SeeOther)
          assert(otherUser.friendRequests(user) === Friendship)
        }
      }
      case None => assert(false, "Session not valid")
    }
    val responseEntityBody = bodyWithFormKey("&friendRequestId=" + user.id + "&friendRequestKind=Friendship&friendRequestAccept=Accept")
    val responseEntity = HttpEntity(ContentType(MediaTypes.`application/x-www-form-urlencoded`, HttpCharsets.`UTF-8`), responseEntityBody)

    otherUser.session match {
      case Some(session) => {
        val responsePost = Post("/requests", responseEntity) ~> addHeader("Cookie", s"ssas_session=$session")
        responsePost ~> route ~> check {
          assert(response.status === StatusCodes.SeeOther)
          assert(otherUser.friends contains user)
          assert(user.friends contains otherUser)
        }
      }
      case None => assert(false, "Session not valid")
    }
  }

  test("User can request friendship, and user can reject it") {
    val (otherUser, otherPassword) = randomLoggedInUser(true, true)

    val entityBody = bodyWithFormKey("&relationship=Friendship")
    val entity = HttpEntity(ContentType(MediaTypes.`application/x-www-form-urlencoded`, HttpCharsets.`UTF-8`), entityBody)

    user.session match {
      case Some(session) => {
        val post = Post("/profile/" + otherUser.id + "/request", entity) ~> addHeader("Cookie", s"ssas_session=$session")
        post ~> route ~> check {
          assert(response.status === StatusCodes.SeeOther)
          assert(otherUser.friendRequests(user) === Friendship)
        }
      }
      case None => assert(false, "Session not valid")
    }
    val responseEntityBody = bodyWithFormKey("&friendRequestId=" + user.id + "&friendRequestKind=Friendship&friendRequestReject=Reject")
    val responseEntity = HttpEntity(ContentType(MediaTypes.`application/x-www-form-urlencoded`, HttpCharsets.`UTF-8`), responseEntityBody)

    otherUser.session match {
      case Some(session) => {
        val responsePost = Post("/requests", responseEntity) ~> addHeader("Cookie", s"ssas_session=$session")
        responsePost ~> route ~> check {
          assert(response.status === StatusCodes.SeeOther)
          assert(!(otherUser.friends contains user))
          assert(!(user.friends contains otherUser))
        }
      }
      case None => assert(false, "Session not valid")
    }
  }

  test("Added hobby appears as hobby on user") {
    val hobby = randomHobby

    val entityBody = bodyWithFormKey("&profileNewHobby=" + hobby) 
    val entity = HttpEntity(ContentType(MediaTypes.`application/x-www-form-urlencoded`, HttpCharsets.`UTF-8`), entityBody)

    user.session match {
      case Some(session) => {
        val post = Post(s"/profile/" + user.id + "/edit/hobby/add", entity) ~> addHeader("Cookie", s"ssas_session=$session")
        post ~> route ~> check {
          assert(response.status === StatusCodes.SeeOther)
          assert(user.hasHobby(hobby))
        }
      }
      case None => assert(false)
    }
  }

  test("Removing a hobby causes a hobby to be removed") {
    val hobby = randomHobby

    user.addHobby(hobby)

    assert(user.hasHobby(hobby))

    val entityBody = bodyWithFormKey("&profileHobby=" + hobby) 
    val entity = HttpEntity(ContentType(MediaTypes.`application/x-www-form-urlencoded`, HttpCharsets.`UTF-8`), entityBody)

    user.session match {
      case Some(session) => {
          val post = Post(s"/profile/" + user.id + "/edit/hobby/remove", entity) ~> addHeader("Cookie", s"ssas_session=$session")
          post ~> route ~> check {
            assert(response.status === StatusCodes.SeeOther)
            assert(!user.hasHobby(hobby))
          }
        }
      case None => assert(false)
    }
  }

  test("You can hug a friend") {
    val (user1, password1) = randomLoggedInUser(true, true)
    val (user2, password2) = randomLoggedInUser(true, true)
    user1.requestFriendship(user2, Friendship)
    user2.acceptFriendRequest(user1, Friendship)

    assert(user1.friends contains user2)
    assert(user2.friends contains user1)

    val entityBody = bodyWithFormKey("")
    val entity = HttpEntity(ContentType(MediaTypes.`application/x-www-form-urlencoded`, HttpCharsets.`UTF-8`), entityBody)

    val unseen = user2.unseenHugs

    user1.session match {
      case Some(session) => {
          val post = Post(s"/hug/" + user2.id, entity) ~> addHeader("Cookie", s"ssas_session=$session")
          post ~> route ~> check {
            assert(response.status === StatusCodes.SeeOther)
            assert(user2.unseenHugs === unseen+1)
          }
        }
      case None => assert(false)
    }
  }

	test("Admin rejects unauthorized access attempts") {
		Get("/admin") ~> addHeader("Cookie", "ssas_session=invalid_session") ~> route ~> check { 
			assert(response.status === StatusCodes.Unauthorized)
		}
	}

  test("User can view profile page of other user") {
    val (loggedInUser, _) = randomLoggedInUser(true, true)

    val entityBody = bodyWithFormKey("")
    val entity = HttpEntity(ContentType(MediaTypes.`application/x-www-form-urlencoded`, HttpCharsets.`UTF-8`), entityBody)

    loggedInUser.session match {
      case Some(session) => {
        val get = Get(s"/profile/${user.id}", entity) ~> addHeader("Cookie", s"ssas_session=$session")
        get ~> route ~> check {
          assert(response.status === StatusCodes.OK)
        }
      }
      case None => assert(false)
    }
  }

  test("Admin can create new users") {
    val name  = randomName
    val email = randomEmail
    val pass  = randomPassword
    val addr  = randomAddress

    val (admin, _) = randomLoggedInUser(true, true, true)

    val entityBody = bodyWithFormKey("&signupEmail=" + email + "&signupName=" + URLEncoder.encode(name, "UTF-8") + "&signupPassword=" + URLEncoder.encode(pass, "UTF-8") + "&signupPasswordConfirm=" + URLEncoder.encode(pass, "UTF-8")) 
    val entity = HttpEntity(ContentType(MediaTypes.`application/x-www-form-urlencoded`, HttpCharsets.`UTF-8`), entityBody)
    admin.session match {
      case Some(session) => 
        val post = Post("/admin/createUser", entity) ~> addHeader("Cookie", s"ssas_session=$session")
    
        post ~> route ~> check {
          assert(response.status === StatusCodes.SeeOther)
        }
      case None => assert(false)
    }
  }

  test("Admin can delete a user") {
    val (admin, _) = randomLoggedInUser(true, true, true)

    val entityBody = bodyWithFormKey("")
    val entity = HttpEntity(ContentType(MediaTypes.`application/x-www-form-urlencoded`, HttpCharsets.`UTF-8`), entityBody)
    admin.session match {
      case Some(session) => 
        val post = Post(s"/admin/delete/${user.id}", entity) ~> addHeader("Cookie", s"ssas_session=$session")
    
        post ~> route ~> check {
          assert(response.status === StatusCodes.SeeOther)
          assert(!(User.all contains user))
        }
      case None => assert(false)
    }
  }

  test("Admin can view a profile page of a user") {
    val (admin, _) = randomLoggedInUser(true, true, true)

    val entityBody = bodyWithFormKey("")
    val entity = HttpEntity(ContentType(MediaTypes.`application/x-www-form-urlencoded`, HttpCharsets.`UTF-8`), entityBody)

    admin.session match {
      case Some(session) => {
        val get = Get(s"/profile/${user.id}", entity) ~> addHeader("Cookie", s"ssas_session=$session")
        get ~> route ~> check {
          assert(response.status === StatusCodes.OK)
        }
      }
      case None => assert(false)
    }
  }

  test("Admin can promote another user to admin") {
    val (admin, _) = randomLoggedInUser(true, true, true)

    val entityBody = bodyWithFormKey("")
    val entity = HttpEntity(ContentType(MediaTypes.`application/x-www-form-urlencoded`, HttpCharsets.`UTF-8`), entityBody)
    admin.session match {
      case Some(session) => 
        val post = Post(s"/admin/toggleAdmin/${user.id}", entity) ~> addHeader("Cookie", s"ssas_session=$session")
    
        post ~> route ~> check {
          assert(response.status === StatusCodes.SeeOther)
          assert(user.admin)
        }
      case None => assert(false)
    }
  }

  test("Admin can demote another admin to regular user") {
    val (admin, _) = randomLoggedInUser(true, true, true)
    val (admin2, _) = randomLoggedInUser(true, true, true)

    val entityBody = bodyWithFormKey("")
    val entity = HttpEntity(ContentType(MediaTypes.`application/x-www-form-urlencoded`, HttpCharsets.`UTF-8`), entityBody)
    admin.session match {
      case Some(session) => 
        val post = Post(s"/admin/toggleAdmin/${admin2.id}", entity) ~> addHeader("Cookie", s"ssas_session=$session")
    
        post ~> route ~> check {
          assert(response.status === StatusCodes.SeeOther)
          assert(!admin2.admin)
        }
      case None => assert(false)
    }
  }

  test("API keys can be created and work. Stops working after being revoked.") {
    val key = ApiKey.create()

    val entityBody = bodyWithFormKey("")
    val entity = HttpEntity(ContentType(MediaTypes.`application/x-www-form-urlencoded`, HttpCharsets.`UTF-8`), entityBody)

    val get = Get(s"/api/user/${user.id}", entity) ~> addHeader("Authorization", key.key.toString())

    get ~> route ~> check {
      assert(response.status === StatusCodes.OK)
    }

    key.revoked = true

    get ~> route ~> check {
      assert(rejection != null)
    }
  }
}
