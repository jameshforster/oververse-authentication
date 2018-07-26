package controllers

import helpers.UnitSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.mvc.ControllerComponents
import play.api.mvc.Results._

import scala.concurrent.Future

class BackendControllerSpec extends UnitSpec with GuiceOneAppPerSuite {

  val testController: BackendController = new BackendController {
    override protected def controllerComponents: ControllerComponents = fakeApplication().injector.instanceOf[ControllerComponents]
  }

  "Calling JsonAction.async" should {

    "execute code that consumes the json model" when {

      "the json is successfully validated" in {
        val result = testController.JsonAction.async[Int](model => implicit request => Future.successful(Ok(Json.toJson(model))))(implicitly)(fakeRequestWithBody(Json.toJson(5)))

        statusOf(result) shouldBe OK
        bodyOf(result) shouldBe "5"
      }
    }

    "return a BadRequest" when {

      "provided with an empty body" in {
        val result = testController.JsonAction.async[Int](model => implicit request => Future.successful(Ok(Json.toJson(model))))(implicitly)(fakeRequest)

        statusOf(result) shouldBe BAD_REQUEST
        bodyOf(result) shouldBe "Missing json body"
      }

      "provided with non-matching json" in {
        val result = testController.JsonAction.async[Int](model => implicit request => Future.successful(Ok(Json.toJson(model))))(implicitly)(fakeRequestWithBody(Json.toJson("A")))

        statusOf(result) shouldBe BAD_REQUEST
        bodyOf(result) shouldBe "Invalid json errors: List((,List(JsonValidationError(List(error.expected.jsnumber),WrappedArray()))))"
      }
    }
  }
}
