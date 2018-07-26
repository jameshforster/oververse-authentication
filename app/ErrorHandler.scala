import com.google.inject.Singleton
import models.exceptions.AuthorisationException
import play.api.http.DefaultHttpErrorHandler
import play.api.mvc.{RequestHeader, Result, Results}

import scala.concurrent.Future

@Singleton
class ErrorHandler extends DefaultHttpErrorHandler with Results {

  override def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
    exception match {
      case e: AuthorisationException => authorisationExceptionResult(e)
      case _ => super.onServerError(request, exception)
    }
  }

  def authorisationExceptionResult(authorisationException: AuthorisationException): Future[Result] = {
    Future.successful(new Status(authorisationException.getStatus)(authorisationException.getMessage))
  }
}
