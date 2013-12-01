package dk.itu.ssas.services

import akka.actor.{ Actor, Props }
import dk.itu.ssas.model.UserExceptions

trait UserService extends SsasService with UserExceptions with Actor {
  import dk.itu.ssas.model._
  import dk.itu.ssas.page._
  import dk.itu.ssas.page.request._
  import dk.itu.ssas.remotes._
  import dk.itu.ssas.Settings.{ baseUrl, remoteTimeout }
  import dk.itu.ssas.Validate._
  import scala.concurrent.{ Await, Future }
  import scala.concurrent.duration._
  import scala.language.postfixOps
  import spray.client.pipelining._
  import spray.http._
  import spray.httpx.SprayJsonSupport._
  import spray.json._
  import spray.routing._
  import spray.routing.HttpService._

  val remotes: List[RemoteSite] = List(new BrocialNetwork(context.system))
  private implicit val executionContext = context.system.dispatcher

  val userRoute = {
    path("requests") {
      get {
        getRequests()
      } ~
      post {
        postRequests()
      }
    } ~
    pathPrefix("profile" / IntNumber) { id =>
      pathEnd {
        get {
          getProfile(id)
        }
      }~
      pathPrefix("edit") {
        path("info") {
          post {
            editInfo(id)
          }
        }~
        pathPrefix("hobby") {
          path("add") {
            post {
              addHobby(id)
            }
          }~
          path("remove") {
            post {
              removeHobby(id)
            }
          }
        }
      } ~
      path("request") {
        post {
          requestRelationship(id)
        }
      }
    } ~
    path("friends") {
      get {
        getFriends()
      } ~
      post {
        postFriends()
      }
    } ~
    path("search") {
      post {
        search()
      }
    } ~
    pathPrefix("hugs") {
      pathEnd {
        get {
          getHugs()
        }
      }~
      path("seen") {
        post { 
          seenHugs()
        }
      }
    }~
    path("hug" / IntNumber) { id =>
      post { 
        hug(id)
      }
    }
  }

  private def getRequests(): ReqCon = withUser { implicit su =>
    html { implicit formKey =>
      complete {
        log.debug(s"Showing friend requests for user $u with session $s")
        ViewRequestsPage("Your friend requests", NoRequest())
      }
    }
  }

  private def postRequests(): ReqCon = withUser { implicit su =>
    withFormKey {
      val fields = ('friendRequestId, 'friendRequestKind, 
                    'friendRequestAccept ?, 'friendRequestReject ?)

      formFields(fields) {
        (friendId, requestKind, accept, reject) =>
        User(friendId.toInt) match {
          case Some(otherUser) =>
            try {
              (accept, reject) match {
                case (Some(_), None) => {
                  log.debug(s"User $u with session $s accepted friend request from user $otherUser")
                  val k = u.acceptFriendRequest(otherUser, Relationship(requestKind))
                }
                case (None, Some(_)) => {
                  log.debug(s"User $u with session $s rejected friend request from user $otherUser")
                  val k = u.rejectFriendRequest(otherUser, Relationship(requestKind))
                }
                case (_,_) => {
                  log.warn(s"Invalid friend request attempted by user $u with session $s")
                  complete {
                    HttpResponse(StatusCodes.BadRequest, 
                                "Both or neither accept and reject was pushed.")
                  }
                }
              }
              redirect(s"$baseUrl/requests", StatusCodes.SeeOther)
            } catch {
              case rde: RelationshipDeserializationException => complete {
                log.error(s"Invalid reationship sent by user $u with session $s")
                HttpResponse(StatusCodes.InternalServerError, "Invalid relationship.")
              }
            }
          case None => complete {
            log.error(s"User $u with session $s tried to accept or reject request from nonexistant user $friendId")
            HttpResponse(StatusCodes.BadRequest, "User not found.")
          }
        }
      }
    }
  }

  private def getProfile(id: Int): ReqCon = withUser { implicit su =>
    if (u.id == id) {
      html { implicit formKey =>
        complete {
          log.debug(s"Showing own profile for user $u, with session $s")
          EditProfilePage("Profile: " + u.name, NoRequest())
        }
      }
    } else {
      val otherUser = User(id)
      otherUser match {
        case Some(other) => html { implicit formKey =>
          complete {
            log.debug(s"Showing profile for $other to $u, with session $s")
            ProfilePage("Profile: " + other.name, ProfilePageRequest(other))
          }
        }
        case None =>  {
          log.warn(s"User $s with session $s tried to view profile of nonexistant user $id")
          error(StatusCodes.NotFound, "The requested user does not exist.")
        }
      }
    }
  }

  private def editInfo(id: Int): ReqCon = withUser { implicit su =>
    withFormKey {
      if (u.id == id) {
        val fields = ('profileName, 'profileAddress, 
                      'profileCurrentPassword, 'profileNewPassword, 
                      'profileNewPasswordConfirm)

        formFields(fields) {
          (name, addr, currentPassword, newPassword, confirmPassword) =>
          val nameChanged     = u.name != name
          val addressChanged  = u.address != addr
          val passwordChanged = newPassword != "" || confirmPassword != ""
          try {
            if (validPassword(currentPassword)) {
              if (u.checkPassword(currentPassword)) {
                if (nameChanged) {
                  if (validName(name)) {
                    u.name = name
                  } else {
                    error(StatusCodes.BadRequest, 
                          "The new name is invalid.")
                  }
                }

                if (addr.isEmpty) {
                  u.address = None
                } else {
                  if (addressChanged) {
                    if (validAddress(Some(addr))) {
                      u.address = Some(addr)
                    } else {
                      error(StatusCodes.BadRequest, 
                            "The new address is invalid.")
                    }
                  }
                }

                if (passwordChanged) {
                  if (newPassword == confirmPassword && 
                      validPassword(newPassword)) {
                    u.password = newPassword
                  } else error(StatusCodes.BadRequest, 
                        "The password could not be changed.")
                }

                redirect(s"$baseUrl/profile/${u.id}", StatusCodes.SeeOther)
              } else error(StatusCodes.Unauthorized, 
                    "You do not have permission to edit this profile.")
            } else error(StatusCodes.BadRequest, "Invalid password.")      
          } catch {
            case dbe: DbError => error(StatusCodes.InternalServerError, 
                                       "Database error.")
            case ue: UserException => error(StatusCodes.BadRequest,
                                            "Invalid info.")
          }
        }
      } else complete {
          HttpResponse(StatusCodes.Unauthorized, 
                      "You cannot edit another person's profile.")
      }
    }
  }

  private def addHobby(id: Int): ReqCon = withUser { implicit su =>
    withFormKey {
      if (u.id == id) {
        formFields('profileNewHobby) { hobby =>
          try {
            if (validHobby(hobby)) {
              u.addHobby(hobby)
              redirect(s"$baseUrl/profile/${u.id}", StatusCodes.SeeOther)
            } else error(StatusCodes.BadRequest, "Invalid hobby.")
          } catch {
            case dbe: DbError => error(StatusCodes.InternalServerError, 
                                       "Database error.")
            case ue: UserException => error(StatusCodes.BadRequest, 
                                            "Invalid info.")
          }
        }
      } else complete {
        HttpResponse(StatusCodes.Unauthorized,
                     "You cannot edit another person's profile.")
      }
    }
  }

  private def removeHobby(id: Int): ReqCon = withUser { implicit su =>
    withFormKey {
      if (u.id == id) {
        formFields('profileHobby) { hobby =>
          try {
            if (validHobby(hobby)) {
              if (u.hobbies.exists { h => h == hobby }) {
                u.removeHobby(hobby)
                redirect(s"$baseUrl/profile/${u.id}", StatusCodes.SeeOther)
              }
            }
            redirect(s"$baseUrl/profile/${u.id}", StatusCodes.SeeOther)
          } catch {
            case dbe: DbError => error(StatusCodes.InternalServerError, 
                                       "Database error.")
            case ue: UserException => error(StatusCodes.BadRequest, 
                                            "Invalid info.")
          }
        }
      } else complete {
        HttpResponse(StatusCodes.Unauthorized, 
                     "You cannot edit another person's profile.")              
      }
    }
  }

  private def requestRelationship(id: Int): ReqCon = withUser { implicit su =>  
    withFormKey {
      if (u.id == id) {
        complete {
          HttpResponse(StatusCodes.BadRequest, 
                      "Sorry, you cannot have a relationship with yourself.")
        }
      } else {
        formFields('relationship) { relationship =>
          try {
            val otherUser = User(id)
            otherUser match {
              case Some(other) =>
                u.requestFriendship(other, Relationship(relationship))
                redirect(s"$baseUrl/profile/${u.id}", StatusCodes.SeeOther)
              case None => complete {
                HttpResponse(StatusCodes.BadRequest, 
                            "User does not exist.")  
              }
            }
          } catch {
            case e: RelationshipDeserializationException => complete {
              HttpResponse(StatusCodes.InternalServerError, 
                          "Invalid relationship.")
            }
          }
        }
      }
    }
  }

  private def getFriends(): ReqCon = withUser { implicit su =>
    html { implicit formKey => 
      complete {
        FriendsPage("Your friends", NoRequest())
      }
    }
  }

  private def postFriends(): ReqCon = withUser { implicit su =>
    withFormKey {
      formFields('friendRemoveId) { friendId =>
        User(friendId.toInt) match {
          case Some(otherUser) =>
            u.removeFriend(otherUser)
            redirect(s"$baseUrl/friends", StatusCodes.SeeOther)
          case None => complete { 
            HttpResponse(StatusCodes.BadRequest, "User not found.")
          }
        }
      }
    }
  }

  private def search(): ReqCon = withUser { implicit su =>
    withFormKey {
      formFields('searchTerm) { searchTerm =>
        if (validHobby(searchTerm) || validEmail(searchTerm)) {
          val remoteSearches = for (r <- remotes) yield (r, r.search(searchTerm))
          val localUsers = u search searchTerm

          html { implicit formKey =>
            complete {
              val remoteUsers: Map[String, List[RemoteUser]] = {
                for ((site, search) <- remoteSearches) yield {
                  try {
                    site.name -> Await.result(search, remoteTimeout seconds)
                  } catch {
                    case e: Exception => {
                      log.error(s"Request to ${site.name} failed: ${e}")
                      site.name -> List()
                    }
                  }
                }
              } toMap

              SearchPage("Search results", SearchPageRequest(localUsers, remoteUsers))
            }
          }
        }
        else {
          complete {
            HttpResponse(StatusCodes.BadRequest, "Search term is improperly formatted")
          }
        }
      }
    }
  }

  private def getHugs(): ReqCon = withUser { implicit su =>
    html { implicit formKey =>
      complete {
        val site = HugsPage("Your hugs", NoRequest())
        u.seenHugs
        site
      }
    }
  }

  private def seenHugs(): ReqCon = withUser { implicit su =>
    withFormKey {
      u.seenHugs
      complete {
        HttpResponse(StatusCodes.OK)
      }
    }
  }

  private def hug(id: Int): ReqCon = withUser { implicit su =>
    withFormKey {
      User(id) match {
        case Some(user) => 
          if(u.friends contains user) {
            try {
              u hug user
            } catch {
              case se: StrangerException => complete {
                HttpResponse(StatusCodes.Unauthorized, "You can only hug your friends.")
              }
            }
            redirect(s"$baseUrl/profile/${u.id}", StatusCodes.SeeOther)
          } else {
            complete {
              HttpResponse(StatusCodes.Unauthorized, "You can only hug your friends.")
            }
          }
        case None => complete {
          HttpResponse(StatusCodes.BadRequest, "User not found.")
        }
      }
    }
  }
}
