package util

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}

object Retry {

  private[this] def retryPromise[T](times: Int, promise: Promise[T], failure: Option[Throwable],
                                    f: => Future[T])(implicit ec: ExecutionContext): Unit = {
    (times, failure) match {
      case (0, Some(e)) =>
        promise.tryFailure(e)
      case (0, None) =>
        promise.tryFailure(new RuntimeException("Failure, but lost track of exception :-("))
      case (_, _) =>
        f.onComplete {
          case Success(t) =>
            promise.trySuccess(t)
          case Failure(e) =>
            retryPromise[T](times - 1, promise, Some(e), f)
        }(ec)
    }
  }

  def retry[T](times: Int)(f: => Future[T])(implicit ec: ExecutionContext): Future[T] = {
    val promise = Promise[T]()
    retryPromise[T](times, promise, None, f)
    promise.future
  }
}
