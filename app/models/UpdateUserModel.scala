package models

import play.api.libs.json.{Json, OFormat}

case class UpdateUserModel(username: String, password: String, updatedValue: String)

object UpdateUserModel {
  implicit val formats: OFormat[UpdateUserModel] = Json.format[UpdateUserModel]
}