package models

import play.api.libs.json.{Json, OFormat}

case class LoginModel(username: String, password: String)

object LoginModel {
  implicit val formats: OFormat[LoginModel] = Json.format[LoginModel]
}
