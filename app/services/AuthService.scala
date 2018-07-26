package services

import java.util.UUID

import com.google.inject.{Inject, Singleton}
import connectors.MongoConnector
import models.exceptions.{ConflictingRecordException, InvalidDetailsException}
import models.{EncryptedUserModel, UserModel}
import play.api.libs.json.{JsObject, Json}
import reactivemongo.api.commands.UpdateWriteResult
import utils.AuthLevels

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class AuthService @Inject()(encryptionService: EncryptionService, mongoConnector: MongoConnector) {

  private[services] def updateToken(userModel: UserModel): UserModel = {
    val token = UUID.randomUUID().toString
    userModel.copy(authToken = Some(token))
  }

  def register(userModel: UserModel): Future[Option[String]] = {
    mongoConnector.findData[EncryptedUserModel]("users", JsObject(Map("username" -> Json.toJson(userModel.username)))).flatMap {
      case Some(_) => throw new ConflictingRecordException(userModel.username)
      case None =>
        val newUser = updateToken(userModel)
        mongoConnector.saveData("users", encryptionService.encryptUser(newUser), JsObject(Map("username" -> Json.toJson(userModel.username)))).map {
          _ => newUser.authToken
        }
    }
  }

  def login(username: String, password: String): Future[Option[String]] = {
    mongoConnector.findData[EncryptedUserModel]("users", JsObject(Map("username" -> Json.toJson(username)))).flatMap {
      case Some(eUser) =>
        val user = encryptionService.decryptUser(eUser)
        if (user.password == password) {
          val updatedUser = updateToken(user)
          mongoConnector.saveData("users", encryptionService.encryptUser(updatedUser), JsObject(Map("username" -> Json.toJson(username)))).map {
            _ => updatedUser.authToken
          }
        } else throw new InvalidDetailsException
      case None => throw new InvalidDetailsException
    }
  }

  def changePassword(username: String, oldPassword: String, newPassword: String): Future[UpdateWriteResult] = {
    mongoConnector.findData[EncryptedUserModel]("users", JsObject(Map("username" -> Json.toJson(username)))).flatMap {
      case Some(eUser) =>
        val user = encryptionService.decryptUser(eUser)
        if (user.password == oldPassword) {
          val updatedUser = user.copy(password = newPassword)
          mongoConnector.saveData("users", encryptionService.encryptUser(updatedUser), JsObject(Map("username" -> Json.toJson(username))))
        } else throw new InvalidDetailsException
      case None => throw new InvalidDetailsException
    }
  }

  def updateEmail(username: String, password: String, newEmail: String): Future[UpdateWriteResult] = {
    mongoConnector.findData[EncryptedUserModel]("users", JsObject(Map("username" -> Json.toJson(username)))).flatMap {
      case Some(eUser) =>
        val user = encryptionService.decryptUser(eUser)
        if (user.password == password) {
          val updatedUser = user.copy(email = newEmail)
          mongoConnector.saveData("users", encryptionService.encryptUser(updatedUser), JsObject(Map("username" -> Json.toJson(username))))
        } else throw new InvalidDetailsException
      case None => throw new InvalidDetailsException
    }
  }

  def authorise(token: String, requiredLevel: Int = AuthLevels.verified): Future[Boolean] = {
    mongoConnector.findData[EncryptedUserModel]("users", JsObject(Map("authToken" -> Json.toJson(encryptionService.encrypt(token))))).map {
      case Some(eUser) =>
        val user = encryptionService.decryptUser(eUser)
        user.authToken.exists(_.equals(token) && user.authLevel >= requiredLevel)
      case None => false
    }
  }
}
