package services

import helpers.UnitSpec
import models.{EncryptedString, EncryptedUserModel, UserModel}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration

class EncryptionServiceSpec extends UnitSpec with GuiceOneAppPerSuite {
  val config: Configuration = fakeApplication().injector.instanceOf[Configuration]
  val service = new EncryptionService(config)

  "Calling .encrypt" should {

    "return an encrypted array" when {

      "supplied with a string of 'password'" in {
        service.encrypt("password") shouldBe EncryptedString("07564fbbdbea70502b93980393cfe617")
      }

      "supplied with a string of 'email@example.com'" in {
        service.encrypt("email@example.com") shouldBe EncryptedString("f419b472b46a05e98dadc5e6f8f0f1148aebdb41c50aa1e07146a39f39c9cdeb")
      }
    }
  }

  "Calling .decrypt" should {

    "return a string" when {

      "supplied with an encrypted string for 'password'" in {
        service.decrypt(EncryptedString("07564fbbdbea70502b93980393cfe617")) shouldBe "password"
      }

      "supplied with an encrypted string for 'email@example.com'" in {
        service.decrypt(EncryptedString("f419b472b46a05e98dadc5e6f8f0f1148aebdb41c50aa1e07146a39f39c9cdeb")) shouldBe "email@example.com"
      }
    }
  }

  "Calling .encryptUser" should {

    "return an EncryptedUser" when {

      "supplied with a UserModel" in {
        service.encryptUser(UserModel("username", "email@example.com", "password", 0, Some("token"))) shouldBe EncryptedUserModel(
          "username",
          EncryptedString("f419b472b46a05e98dadc5e6f8f0f1148aebdb41c50aa1e07146a39f39c9cdeb"),
          EncryptedString("07564fbbdbea70502b93980393cfe617"),
          0,
          Some(EncryptedString("97cbf81be1c75c63f09bf66291c312eb"))
        )
      }
    }
  }

  "Calling .decryptUser" should {

    "return a User" when {

      "provided with an EncryptedUser" in {
        service.decryptUser(EncryptedUserModel(
          "username",
          EncryptedString("f419b472b46a05e98dadc5e6f8f0f1148aebdb41c50aa1e07146a39f39c9cdeb"),
          EncryptedString("07564fbbdbea70502b93980393cfe617"),
          0,
          Some(EncryptedString("97cbf81be1c75c63f09bf66291c312eb"))
        )) shouldBe UserModel("username", "email@example.com", "password", 0, Some("token"))
      }
    }
  }
}
