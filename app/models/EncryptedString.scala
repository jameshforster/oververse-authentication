package models

import play.api.libs.json.{Json, OFormat}

case class EncryptedString(data: String)

object EncryptedString {
  implicit val formats: OFormat[EncryptedString] = Json.format[EncryptedString]
}
