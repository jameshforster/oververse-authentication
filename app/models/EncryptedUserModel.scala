package models

import play.api.libs.json.{Json, OFormat}

case class EncryptedUserModel(username: String, email: EncryptedString, password: EncryptedString, authLevel: Int, authToken: Option[EncryptedString] = None)

object EncryptedUserModel {
  implicit val formats: OFormat[EncryptedUserModel] = Json.format[EncryptedUserModel]
}
