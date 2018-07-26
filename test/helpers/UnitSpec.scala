package helpers

import java.nio.charset.Charset

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.JsValue
import play.api.mvc.{AnyContent, AnyContentAsJson, Result}
import play.api.test.FakeRequest

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

trait UnitSpec extends WordSpec with Matchers {

  implicit val system: ActorSystem = ActorSystem("QuickStart")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val fakeRequest: FakeRequest[AnyContent] = FakeRequest()

  def fakeRequestWithBody(json: JsValue): FakeRequest[AnyContentAsJson] = FakeRequest().withJsonBody(json)

  def statusOf(result: Future[Result]): Int = {
    await(result).header.status
  }

  def bodyOf(result: Future[Result])(implicit mat: Materializer): String = {
    val body = await(result).body
    await(body.consumeData).decodeString(Charset.defaultCharset().name())
  }

  def await[T](future: Future[T]): T = {
    Await.result(future, Duration.apply(5, SECONDS))
  }
}
