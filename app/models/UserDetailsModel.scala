package models

import play.api.libs.json.{Json, OFormat}

case class UserDetailsModel(username: String, email: String, authLevel: Int)

object UserDetailsModel {
  implicit val formats: OFormat[UserDetailsModel] = Json.format[UserDetailsModel]
}
