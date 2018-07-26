package connectors

import com.google.inject.{Inject, Singleton}
import play.api.libs.json._
import play.modules.reactivemongo.{ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.api.commands.UpdateWriteResult
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.modules.reactivemongo.json._

@Singleton
class MongoConnector @Inject()(val reactiveMongoApi: ReactiveMongoApi) extends ReactiveMongoComponents {
  private lazy val database = reactiveMongoApi.database

  def saveData[A](key: String, data: A, identifier: JsObject)(implicit writes: Writes[A]): Future[UpdateWriteResult] = {
    database.flatMap { x =>
      x.collection[JSONCollection](key).update(identifier, Json.toJson(data).asInstanceOf[JsObject], upsert = true)
    }
  }

  def findData[A](key: String, identifier: JsObject)(implicit reads: Reads[A]): Future[Option[A]] = {
    database.flatMap { x =>
      x.collection[JSONCollection](key).find[JsObject](identifier).cursor[JsObject]().headOption.map {
        case Some(json: JsObject) => json.validate[A].asOpt
        case _ => None
      }
    }
  }

  def findAllData[A](key: String, identifier: JsObject)(implicit reads: Reads[A]): Future[Seq[A]] = {
    database.flatMap { x =>
      x.collection[JSONCollection](key).find[JsObject](identifier).cursor[JsObject]().fold(Seq[A]()) { (result, json) =>
        json.validate[A].asOpt match {
          case Some(data) => result ++ Seq(data)
          case _ => result
        }
      }
    }
  }
}
