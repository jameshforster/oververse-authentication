package models.exceptions

class AuthorisationException(status: Int, message: String) extends Exception(message) {
  val getStatus: Int = status
}
