package util

import java.io.InputStream
import java.security.{KeyStore, SecureRandom}
import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManagerFactory}

import akka.http.scaladsl.{ConnectionContext, HttpsConnectionContext}

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}

object HttpsSupport {
  def context(): HttpsConnectionContext = {
    val password: Array[Char] = "uW8WlTlANX0WxAo1PvDnQBGUXB1UeQrVvitD22yLiJxkxtJLz3gFzcVoKu25GJLW".toCharArray

    val ks: KeyStore = KeyStore.getInstance("PKCS12")
    val keystore: InputStream = getClass.getClassLoader.getResourceAsStream("foo.bar.p12")

    require(keystore != null, "Keystore required!")
    ks.load(keystore, password)

    val keyManagerFactory: KeyManagerFactory = KeyManagerFactory.getInstance("SunX509")
    keyManagerFactory.init(ks, password)

    val tmf: TrustManagerFactory = TrustManagerFactory.getInstance("SunX509")
    tmf.init(ks)

    val sslContext: SSLContext = SSLContext.getInstance("TLS")
    sslContext.init(keyManagerFactory.getKeyManagers, tmf.getTrustManagers, new SecureRandom)
    ConnectionContext.https(sslContext)
  }
}

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
