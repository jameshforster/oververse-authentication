package services

import connectors.MongoConnector
import helpers.UnitSpec
import models.exceptions.{ConflictingRecordException, InsufficientAuthLevelException, InvalidDetailsException}
import models.{EncryptedString, EncryptedUserModel, UserDetailsModel, UserModel}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import play.api.libs.json.JsObject
import reactivemongo.api.commands.UpdateWriteResult
import utils.AuthLevels

import scala.concurrent.Future

class AuthServiceSpec extends UnitSpec with MockitoSugar {

  val userModel = UserModel("username", "email@example.com", "password", AuthLevels.verified, Some("token"))
  val encryptedUserModel = EncryptedUserModel("username", EncryptedString("encryptedEmail"), EncryptedString("encryptedPassword"), AuthLevels.verified, Some(EncryptedString("token")))

  def setupService(user: Future[Option[EncryptedUserModel]], save: Future[UpdateWriteResult]): AuthService = {
    val mockConnector = mock[MongoConnector]
    val mockService = mock[EncryptionService]

    when(mockConnector.findData[EncryptedUserModel](ArgumentMatchers.any[String], ArgumentMatchers.any[JsObject])(ArgumentMatchers.any()))
      .thenReturn(user)

    when(mockConnector.saveData(ArgumentMatchers.any[String], ArgumentMatchers.any(), ArgumentMatchers.any[JsObject])(ArgumentMatchers.any()))
      .thenReturn(save)

    when(mockService.encryptUser(ArgumentMatchers.any[UserModel]))
      .thenReturn(encryptedUserModel)

    when(mockService.decryptUser(ArgumentMatchers.any[EncryptedUserModel]))
      .thenReturn(userModel)

    when(mockService.encrypt(ArgumentMatchers.any()))
      .thenReturn(EncryptedString("encryptedString"))

    new AuthService(mockService, mockConnector)
  }

  "Calling the .register method" should {

    "return an auth token" when {

      "registering a new user" in {
        val service = setupService(Future.successful(None), Future.successful(mock[UpdateWriteResult]))
        val result = await(service.register(userModel))

        result.isDefined shouldBe true
        result.get.length shouldBe 36
      }
    }

    "return a conflicting record exception" when {

      "registering an existing user" in {
        val service = setupService(Future.successful(Some(encryptedUserModel)), Future.successful(mock[UpdateWriteResult]))
        val result = the[ConflictingRecordException] thrownBy await(service.register(userModel))

        result should have message "User: username is already in use!"
      }
    }

    "return an exception from mongo" when {

      "the find method returns an exception" in {
        val service = setupService(Future.failed(new Exception("test error")), Future.successful(mock[UpdateWriteResult]))
        val result = the[Exception] thrownBy await(service.register(userModel))

        result should have message "test error"
      }

      "the save method returns an exception" in {
        val service = setupService(Future.successful(None), Future.failed(new Exception("test error")))
        val result = the[Exception] thrownBy await(service.register(userModel.copy(authLevel = 50)))

        result should have message "test error"
      }
    }
  }

  "Calling the .login method" should {

    "return a valid token" when {

      "logging in with a matching user and password" in {
        val service = setupService(Future.successful(Some(encryptedUserModel)), Future.successful(mock[UpdateWriteResult]))
        val result = await(service.login("username", "password"))

        result.isDefined shouldBe true
        result.get.length shouldBe 36
      }
    }

    "return an InvalidDetailsException" when {

      "logging in with an invalid password" in {
        val service = setupService(Future.successful(Some(encryptedUserModel)), Future.successful(mock[UpdateWriteResult]))
        val result = service.login("username", "wrongPassword")

        an[InvalidDetailsException] should be thrownBy await(result)
      }

      "logging in with an invalid user" in {
        val service = setupService(Future.successful(None), Future.successful(mock[UpdateWriteResult]))
        val result = service.login("username", "password")

        an[InvalidDetailsException] should be thrownBy await(result)
      }
    }

    "return an exception from mongo" when {

      "the find method returns an exception" in {
        val service = setupService(Future.failed(new Exception("test error")), Future.successful(mock[UpdateWriteResult]))
        val result = the[Exception] thrownBy await(service.login("username", "password"))

        result should have message "test error"
      }

      "the save method returns an exception" in {
        val service = setupService(Future.successful(Some(encryptedUserModel)), Future.failed(new Exception("test error")))
        val result = the[Exception] thrownBy await(service.login("username", "password"))

        result should have message "test error"
      }
    }
  }

  "Calling the .changePassword method" should {

    "return a success" when {

      "a matching user and password is supplied" in {
        val service = setupService(Future.successful(Some(encryptedUserModel)), Future.successful(mock[UpdateWriteResult]))
        val result = await(service.changePassword("username", "password", "newPassword"))

        noException should be thrownBy result
      }
    }

    "return an InvalidDetailsException" when {

      "logging in with an invalid password" in {
        val service = setupService(Future.successful(Some(encryptedUserModel)), Future.successful(mock[UpdateWriteResult]))
        val result = service.changePassword("username", "wrongPassword", "newPassword")

        an[InvalidDetailsException] should be thrownBy await(result)
      }

      "logging in with an invalid user" in {
        val service = setupService(Future.successful(None), Future.successful(mock[UpdateWriteResult]))
        val result = service.changePassword("username", "password", "newPassword")

        an[InvalidDetailsException] should be thrownBy await(result)
      }
    }

    "return an exception from mongo" when {

      "the find method returns an exception" in {
        val service = setupService(Future.failed(new Exception("test error")), Future.successful(mock[UpdateWriteResult]))
        val result = the[Exception] thrownBy await(service.changePassword("username", "password", "newPassword"))

        result should have message "test error"
      }

      "the save method returns an exception" in {
        val service = setupService(Future.successful(Some(encryptedUserModel)), Future.failed(new Exception("test error")))
        val result = the[Exception] thrownBy await(service.changePassword("username", "password", "newPassword"))

        result should have message "test error"
      }
    }
  }

  "Calling the .updateEmail method" should {

    "return a success" when {

      "a matching user and password is supplied" in {
        val service = setupService(Future.successful(Some(encryptedUserModel)), Future.successful(mock[UpdateWriteResult]))
        val result = await(service.updateEmail("username", "password", "newEmail"))

        noException should be thrownBy result
      }
    }

    "return an InvalidDetailsException" when {

      "logging in with an invalid password" in {
        val service = setupService(Future.successful(Some(encryptedUserModel)), Future.successful(mock[UpdateWriteResult]))
        val result = service.updateEmail("username", "wrongPassword", "newEmail")

        an[InvalidDetailsException] should be thrownBy await(result)
      }

      "logging in with an invalid user" in {
        val service = setupService(Future.successful(None), Future.successful(mock[UpdateWriteResult]))
        val result = service.updateEmail("username", "password", "newEmail")

        an[InvalidDetailsException] should be thrownBy await(result)
      }
    }

    "return an exception from mongo" when {

      "the find method returns an exception" in {
        val service = setupService(Future.failed(new Exception("test error")), Future.successful(mock[UpdateWriteResult]))
        val result = the[Exception] thrownBy await(service.updateEmail("username", "password", "newEmail"))

        result should have message "test error"
      }

      "the save method returns an exception" in {
        val service = setupService(Future.successful(Some(encryptedUserModel)), Future.failed(new Exception("test error")))
        val result = the[Exception] thrownBy await(service.updateEmail("username", "password", "newEmail"))

        result should have message "test error"
      }
    }
  }

  "Calling the authorise method" should {

    "return a true" when {

      "found user has a matching authorisation level" in {
        val service = setupService(Future.successful(Some(encryptedUserModel)), Future.successful(mock[UpdateWriteResult]))
        val result = await(service.authorise("token"))

        result shouldBe UserDetailsModel("username", "email@example.com", 100)
      }

      "found user has a greater than required authorisation level" in {
        val service = setupService(Future.successful(Some(encryptedUserModel.copy(authLevel = AuthLevels.moderator))), Future.successful(mock[UpdateWriteResult]))
        val result = await(service.authorise("token"))

        result shouldBe UserDetailsModel("username", "email@example.com", 100)
      }
    }

    "return a false" when {

      "no matching user is found" in {
        val service = setupService(Future.successful(None), Future.successful(mock[UpdateWriteResult]))
        val result = service.authorise("token")

        the[InvalidDetailsException] thrownBy await(result) should have message "Invalid username and/or password"
      }

      "found user has too low an authorisation level" in {
        val service = setupService(Future.successful(Some(encryptedUserModel)), Future.successful(mock[UpdateWriteResult]))
        val result = service.authorise("token", AuthLevels.moderator)

        the[InsufficientAuthLevelException] thrownBy await(result) should have message "Level of 200 is insufficient to access a level of 100 for this resource"
      }
    }
  }
}
