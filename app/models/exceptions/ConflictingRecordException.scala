package models.exceptions

import play.api.http.Status.BAD_REQUEST

class ConflictingRecordException(username: String) extends AuthorisationException(BAD_REQUEST, s"User: $username is already in use!")
