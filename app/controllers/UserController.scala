package controllers

import com.google.inject.{Inject, Singleton}
import models.{LoginModel, UpdateUserModel, UserModel}
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents, Session}
import services.AuthService
import utils.AuthLevels

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class UserController @Inject()(val controllerComponents: ControllerComponents, authService: AuthService) extends BackendController {

  val register: Action[AnyContent] = {
    JsonAction.async[UserModel] { model =>
      implicit request =>
        if (model.authLevel == AuthLevels.unverified) {
          authService.register(model).map { token =>
            Ok(Json.toJson(token))
          }
        } else request2session.get("authToken").map { authToken =>
          authService.authorise(authToken, AuthLevels.admin).flatMap { result =>
            if (result) authService.register(model).map(_ => Ok)
            else Future.successful(Forbidden)
          }
        }.getOrElse(Future.successful(Unauthorized))
    }
  }

  def authorise(requestedAuth: Int): Action[AnyContent] = Action.async { implicit request =>
    request2session.get("authToken").map { authToken =>
      authService.authorise(authToken, requestedAuth).flatMap { result =>
        if (result) Future.successful(Ok)
        else Future.successful(Forbidden)
      }
    }.getOrElse(Future.successful(Unauthorized))
  }

  val login: Action[AnyContent] = {
    JsonAction.async[LoginModel] { model =>
      implicit request =>
        authService.login(model.username, model.password).map {
          case Some(result) => Ok(result)
          case _ => BadRequest
        }
    }
  }

  val changePassword: Action[AnyContent] = {
    JsonAction.async[UpdateUserModel] { model =>
      implicit request =>
        authService.changePassword(model.username, model.password, model.updatedValue).map(_ => Ok)
    }
  }

  val updateEmail: Action[AnyContent] = {
    JsonAction.async[UpdateUserModel] { model =>
      implicit request =>
        authService.updateEmail(model.username, model.password, model.updatedValue).map(_ => Ok)
    }
  }
}
