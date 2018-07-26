package models

import play.api.libs.json.{Json, OFormat}

case class UserModel(username: String, email: String, password: String, authLevel: Int, authToken: Option[String] = None)

object UserModel {
  implicit val formats: OFormat[UserModel] = Json.format[UserModel]
}