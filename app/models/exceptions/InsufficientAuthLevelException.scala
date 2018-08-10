package models.exceptions

class InsufficientAuthLevelException(required: Int, actual: Int) extends AuthorisationException(403, s"Level of $required is insufficient to access a level of $actual for this resource")