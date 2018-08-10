package models.exceptions

class InvalidDetailsException extends AuthorisationException(401, "Invalid username and/or password")
