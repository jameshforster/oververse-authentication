package controllers

import helpers.UnitSpec
import models.exceptions.{AuthorisationException, InsufficientAuthLevelException}
import models.{LoginModel, UpdateUserModel, UserDetailsModel, UserModel}
import org.mockito.ArgumentMatchers
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.mvc.ControllerComponents
import reactivemongo.api.commands.UpdateWriteResult
import services.AuthService
import org.mockito.Mockito._
import play.api.libs.json.Json
import utils.AuthLevels

import scala.concurrent.Future

class UserControllerSpec extends UnitSpec with GuiceOneAppPerSuite with MockitoSugar {

  def setupController(token: Future[Option[String]], result: Future[UpdateWriteResult], authorised: Future[UserDetailsModel]): UserController = {
    val components = fakeApplication().injector.instanceOf[ControllerComponents]
    val mockService = mock[AuthService]

    when(mockService.register(ArgumentMatchers.any[UserModel]))
      .thenReturn(token)

    when(mockService.login(ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
      .thenReturn(token)

    when(mockService.updateEmail(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
      .thenReturn(result)

    when(mockService.changePassword(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
      .thenReturn(result)

    when(mockService.authorise(ArgumentMatchers.anyString(), ArgumentMatchers.anyInt()))
      .thenReturn(authorised)

    new UserController(components, mockService)
  }

  "Calling .register" should {

    "return an ok response" when {

      "a new user is successfully created with a basic authlevel" in {
        val controller = setupController(Future.successful(Some("token")), Future.successful(mock[UpdateWriteResult]), Future.successful(UserDetailsModel("name", "email", 50)))
        val result = controller.register(fakeRequestWithBody(Json.toJson(UserModel("name", "email", "password", AuthLevels.unverified, None))))

        statusOf(result) shouldBe 200
      }

      "an admin creates a user with a higher than basic authlevel" in {
        val controller = setupController(Future.successful(Some("token")), Future.successful(mock[UpdateWriteResult]), Future.successful(UserDetailsModel("name", "email", 50)))
        val result = controller.register(fakeRequestWithBody(Json.toJson(UserModel("name", "email", "password", AuthLevels.verified, None))).withSession("authToken" -> "token"))

        statusOf(result) shouldBe 200
      }
    }

    "return a forbidden exception" when {

      "a non-admin attempts to create a user with a higher than basic authlevel" in {
        val controller = setupController(Future.successful(Some("token")), Future.successful(mock[UpdateWriteResult]), Future.failed(new InsufficientAuthLevelException(200, 50)))
        val result = controller.register(fakeRequestWithBody(Json.toJson(UserModel("name", "email", "password", AuthLevels.verified, None))).withSession("authToken" -> "token"))

        the [InsufficientAuthLevelException] thrownBy await(result) should have message "Level of 200 is insufficient to access a level of 50 for this resource"
      }
    }

    "return an unauthorised response" when {

      "an user without a token attempts to create a user with a higher than basic authlevel" in {
        val controller = setupController(Future.successful(Some("token")), Future.successful(mock[UpdateWriteResult]), Future.failed(new InsufficientAuthLevelException(200, 50)))
        val result = controller.register(fakeRequestWithBody(Json.toJson(UserModel("name", "email", "password", AuthLevels.verified, None))))

        statusOf(result) shouldBe 401
      }
    }

    "throw an exception" when {

      "the registration returns an exception for an unverified user" in {
        val controller = setupController(Future.failed(new Exception("test exception")), Future.successful(mock[UpdateWriteResult]), Future.successful(UserDetailsModel("name", "email", 50)))
        val result = controller.register(fakeRequestWithBody(Json.toJson(UserModel("name", "email", "password", AuthLevels.unverified, None))).withSession("authToken" -> "token"))

        the[Exception] thrownBy await(result) should have message "test exception"
      }

      "the registration returns an exception for a greater than unverified user" in {
        val controller = setupController(Future.failed(new Exception("test exception")), Future.successful(mock[UpdateWriteResult]), Future.successful(UserDetailsModel("name", "email", 50)))
        val result = controller.register(fakeRequestWithBody(Json.toJson(UserModel("name", "email", "password", AuthLevels.verified, None))).withSession("authToken" -> "token"))

        the[Exception] thrownBy await(result) should have message "test exception"
      }

      "the authorisation returns an exception for a greater than unverified user" in {
        val controller = setupController(Future.successful(Some("token")), Future.successful(mock[UpdateWriteResult]), Future.failed(new Exception("test exception")))
        val result = controller.register(fakeRequestWithBody(Json.toJson(UserModel("name", "email", "password", AuthLevels.verified, None))).withSession("authToken" -> "token"))

        the[Exception] thrownBy await(result) should have message "test exception"
      }
    }
  }

  "Calling .authorise" should {

    "return an ok response" when {

      "submitting a request with the required authorisation" in {
        val controller = setupController(Future.successful(Some("token")), Future.successful(mock[UpdateWriteResult]), Future.successful(UserDetailsModel("name", "email", 50)))
        val result = controller.authorise(AuthLevels.verified)(fakeRequest.withJsonBody(Json.toJson("token")))

        statusOf(result) shouldBe 200
      }
    }

    "return a forbidden exception" when {

      "submitting a request without the required authorisation" in {
        val controller = setupController(Future.successful(Some("token")), Future.successful(mock[UpdateWriteResult]), Future.failed(new InsufficientAuthLevelException(200, 50)))
        val result = controller.authorise(AuthLevels.verified)(fakeRequest.withJsonBody(Json.toJson("token")))

        the [InsufficientAuthLevelException] thrownBy await(result) should have message "Level of 200 is insufficient to access a level of 50 for this resource"
      }
    }

    "return a bad request response" when {

      "submitting a request without a token" in {
        val controller = setupController(Future.successful(Some("token")), Future.successful(mock[UpdateWriteResult]), Future.successful(UserDetailsModel("name", "email", 50)))
        val result = controller.authorise(AuthLevels.verified)(fakeRequest)

        statusOf(result) shouldBe 400
      }
    }

    "return an exception" when {

      "the service throws an exception" in {
        val controller = setupController(Future.successful(Some("token")), Future.successful(mock[UpdateWriteResult]), Future.failed(new Exception("test exception")))
        val result = controller.authorise(AuthLevels.verified)(fakeRequest.withJsonBody(Json.toJson("token")))

        the [Exception] thrownBy await(result) should have message "test exception"
      }
    }
  }

  "Calling .login" should {

    "return an ok response" when {

      "provided with the correct details" in {
        val controller = setupController(Future.successful(Some("token")), Future.successful(mock[UpdateWriteResult]), Future.successful(UserDetailsModel("name", "email", 50)))
        val result = controller.login(fakeRequestWithBody(Json.toJson(LoginModel("username", "password"))))

        statusOf(result) shouldBe 200
        bodyOf(result) shouldBe "token"
      }
    }

    "return a bad request response" when {

      "provided with incorrect details" in {
        val controller = setupController(Future.successful(None), Future.successful(mock[UpdateWriteResult]), Future.successful(UserDetailsModel("name", "email", 50)))
        val result = controller.login(fakeRequestWithBody(Json.toJson(LoginModel("username", "password"))))

        statusOf(result) shouldBe 400
      }
    }

    "return an exception" when {

      "the service throws an exception" in {
        val controller = setupController(Future.failed(new Exception("test exception")), Future.successful(mock[UpdateWriteResult]), Future.successful(UserDetailsModel("name", "email", 50)))
        val result = controller.login(fakeRequestWithBody(Json.toJson(LoginModel("username", "password"))))

        the [Exception] thrownBy await(result) should have message "test exception"
      }
    }
  }

  "Calling .changePassword" should {

    "return an ok response" when {

      "provided with the correct details" in {
        val controller = setupController(Future.successful(Some("token")), Future.successful(mock[UpdateWriteResult]), Future.successful(UserDetailsModel("name", "email", 50)))
        val result = controller.changePassword(fakeRequestWithBody(Json.toJson(UpdateUserModel("username", "password", "newPassword"))))

        statusOf(result) shouldBe 200
      }
    }

    "return an exception" when {

      "provided with incorrect details" in {
        val controller = setupController(Future.successful(Some("token")), Future.failed(new Exception("test exception")), Future.successful(UserDetailsModel("name", "email", 50)))
        val result = controller.changePassword(fakeRequestWithBody(Json.toJson(UpdateUserModel("username", "password", "newPassword"))))

        the [Exception] thrownBy await(result) should have message "test exception"
      }
    }
  }

  "Calling .updateEmail" should {

    "return an ok response" when {

      "provided with the correct details" in {
        val controller = setupController(Future.successful(Some("token")), Future.successful(mock[UpdateWriteResult]), Future.successful(UserDetailsModel("name", "email", 50)))
        val result = controller.updateEmail(fakeRequestWithBody(Json.toJson(UpdateUserModel("username", "password", "newPassword"))))

        statusOf(result) shouldBe 200
      }
    }

    "return an exception" when {

      "provided with incorrect details" in {
        val controller = setupController(Future.successful(Some("token")), Future.failed(new Exception("test exception")), Future.successful(UserDetailsModel("name", "email", 50)))
        val result = controller.updateEmail(fakeRequestWithBody(Json.toJson(UpdateUserModel("username", "password", "newPassword"))))

        the [Exception] thrownBy await(result) should have message "test exception"
      }
    }
  }
}