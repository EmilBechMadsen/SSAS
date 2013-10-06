package dk.itu.ssas.services

import dk.itu.ssas.model.UserExceptions

object UserService extends SsasService with UserExceptions {
  import dk.itu.ssas.model._
  import dk.itu.ssas.page._
  import dk.itu.ssas.page.request._
  import dk.itu.ssas.Validate._
  import scala.language.postfixOps
  import spray.http._
  import spray.routing._
  import spray.routing.HttpService._

  def route = {
    path("requests") {
      get {
        withSession { s =>
          withUser(s) { u =>
            html(s) { (s, formKey) =>
              complete {
                ViewRequestsPage.render("Your friend requests", formKey, Some(u), ViewRequestsPageRequest(u))
              }
            }
          }
        }
      } ~
      post {
        withSession { s =>
          withUser(s) { u =>
            withFormKey(s) {
              formFields('friendRequestId, 'friendRequestKind, 'friendRequestAccept ?, 'friendRequestReject ?) {
                (friendId, requestKind, accept, reject) =>
                User(friendId.toInt) match {
                  case Some(otherUser) =>
                    try {
                      (accept, reject) match {
                        case (Some(_), None) => {
                          val k = u.acceptFriendRequest(otherUser, Relationship(requestKind))
                        }
                        case (None, Some(_)) => {
                          val k = u.rejectFriendRequest(otherUser, Relationship(requestKind))
                        }
                        case (_,_) => {
                          complete { HttpResponse(StatusCodes.BadRequest, "Both or neither accept and reject was pushed.") }
                        }
                      }
                      redirect(s"/requests", StatusCodes.SeeOther)
                    } catch {
                      case rde: RelationshipDeserializationException => complete { HttpResponse(StatusCodes.InternalServerError, "Invalid relationship.") }
                    }
                  case None =>
                    complete { HttpResponse(StatusCodes.BadRequest, "User not found.") }
                }
              }
            }
          }
        }
      }
    } ~
    pathPrefix("profile" / IntNumber) { id =>
      path("") {
        get {
          withSession { s =>
            withUser(s) { u =>
              if (u.id == id) {
                html(s) { (s, formKey) =>
                  complete {
                    EditProfilePage.render("Profile: " + u.name, formKey, Some(u), EditProfilePageRequest(u))
                  }
                }
              } else {
                val otherUser = User(id)
                otherUser match {
                  case Some(other) => html(s) { (s, formKey) =>
                    complete {
                      ProfilePage.render("Profile: " + other.name, formKey, Some(u), ProfilePageRequest(u, other))
                    }
                  }
                  case None => complete {
                    HttpResponse(StatusCodes.NotFound, "The requested user does not exist.")
                  }
                }
              }
            }
          }
        }
      }~
      pathPrefix("edit") {
        path("info") {
          post {
            withSession { s =>
              withUser(s) { u =>
                withFormKey(s) {
                  if (u.id == id) {
                    formFields('profileName, 'profileAddress, 'profileCurrentPassword, 'profileNewPassword, 'profileNewPasswordConfirm) {
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
                              } else complete {
                                HttpResponse(StatusCodes.BadRequest, "The new name is invalid.")
                              }
                            }

                            if (addr.isEmpty) {
                              u.address = None
                            } else {
                              if (addressChanged) {
                                if (validAddress(Some(addr))) {
                                  u.address = Some(addr)
                                } else complete {
                                  HttpResponse(StatusCodes.BadRequest, "The new address is invalid.")
                                }
                              }
                            }

                            if (passwordChanged) {
                              if (newPassword == confirmPassword && validPassword(newPassword)) {
                                u.password = newPassword
                              } else complete {
                                HttpResponse(StatusCodes.BadRequest, "The password could not be changed.")
                              }
                            }

                            redirect(s"/profile/${u.id}", StatusCodes.SeeOther)
                          } else complete {
                            HttpResponse(StatusCodes.Unauthorized, "You do not have permission to edit this profile.")
                          }
                        } else complete {
                          HttpResponse(StatusCodes.BadRequest, "Invalid password.")      
                        }
                      } catch {
                        case dbe: DbError => complete { HttpResponse(StatusCodes.InternalServerError, "Database error.") }
                        case ue: UserException => complete { HttpResponse(StatusCodes.BadRequest, "Invalid info.") }
                     }
                    }
                  } else complete {
                      HttpResponse(StatusCodes.Unauthorized, "You cannot edit another person's profile.")
                  }
                }
              }
            }
          }
        }~
        pathPrefix("hobby") {
          path("add") {
            post {
              withSession { s =>
                withUser(s) { u =>
                  withFormKey(s) {
                    if (u.id == id) {
                      formFields('profileNewHobby) { hobby =>
                        try {
                          if (validHobby(hobby)) {
                            u.addHobby(hobby)
                            redirect(s"/profile/${u.id}", StatusCodes.SeeOther)
                          } else complete {
                            HttpResponse(StatusCodes.BadRequest, "Invalid hobby.")
                          }
                        } catch {
                          case dbe: DbError => complete { HttpResponse(StatusCodes.InternalServerError, "Database error.") }
                          case ue: UserException => complete { HttpResponse(StatusCodes.BadRequest, "Invalid info.") }
                        }
                      }
                    } else complete {
                      HttpResponse(StatusCodes.Unauthorized, "You cannot edit another person's profile.")
                    }
                  }
                }
              }
            }
          }~
          path("remove") {
            post {
              withSession { s =>
                withUser(s) { u =>
                  withFormKey(s) {
                    if (u.id == id) {
                      formFields('profileHobby) { hobby =>
                        try {
                          if (validHobby(hobby)) {
                            if (u.hobbies.exists { h => h == hobby }) {
                              u.removeHobby(hobby)
                              redirect(s"/profile/${u.id}", StatusCodes.SeeOther)
                            }
                          }
                          redirect(s"/profile/${u.id}", StatusCodes.SeeOther)
                        } catch {
                          case dbe: DbError => complete { HttpResponse(StatusCodes.InternalServerError, "Database error.") }
                          case ue: UserException => complete { HttpResponse(StatusCodes.BadRequest, "Invalid info.") }
                        }
                      }
                    } else complete {
                      HttpResponse(StatusCodes.Unauthorized, "You cannot edit another person's profile.")              
                    }
                  }
                }
              }
            }
          }
        }~
        path("request") {
          post {
            withSession { s =>
              withUser(s) { u =>  
                withFormKey(s) {
                  if (u.id == id) {
                    complete {
                      HttpResponse(StatusCodes.BadRequest, "Sorry, you cannot have a relationship with yourself.")
                    }
                  } else {
                    formFields('relationship) { relationship =>
                      try {
                        val otherUser = User(id)
                        otherUser match {
                          case Some(other) =>
                            u.requestFriendship(other, Relationship(relationship))
                            redirect(s"/profile/${u.id}", StatusCodes.SeeOther)
                          case None => complete {
                            HttpResponse(StatusCodes.BadRequest, "User does not exist.")  
                          }
                        }
                      } catch {
                        case e: RelationshipDeserializationException => complete {
                          HttpResponse(StatusCodes.InternalServerError, "Invalid relationship.")
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    } ~
    path("friends") {
      get {
        withSession { s =>
          withUser(s) { u =>
            html(s) { (s, formKey) =>
              complete {
                FriendsPage.render("Your friends", formKey, Some(u), FriendsPageRequest(u))
              }
            }
          }
        }
      } ~
      post {
        withSession { s =>
          withUser(s) { u =>
            withFormKey(s) {
              formFields('friendRemoveId) { friendId =>
                User(friendId.toInt) match {
                  case Some(otherUser) =>
                    u.removeFriend(otherUser)
                    redirect(s"/friends", StatusCodes.SeeOther)
                  case None =>
                    complete { HttpResponse(StatusCodes.BadRequest, "User not found.") }
                }
              }
            }
          }
        }
      }
    } ~
    path("search") {
      post {
        withSession { s =>
          withUser(s) { u =>
            withFormKey(s) {
              formFields('searchTerm) { search => 
                val users = u.search(search)
                html(s) { (s, formKey) =>
                  complete {
                    SearchPage.render("Search results", formKey, Some(u), SearchPageRequest(users))
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}