package models.exceptions

class InvalidDetailsException extends AuthorisationException(400, "Invalid username and/or password")
