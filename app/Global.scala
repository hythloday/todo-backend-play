import java.util.concurrent.TimeUnit

import org.joda.time.Instant
import org.joda.time.format.DateTimeFormat

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.iteratee._
import play.api.mvc._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Promise, Future}

object CorsFilter extends Filter {

  val corsHeaders = List(
    "access-control-allow-origin" -> "*",
    "access-control-allow-headers" -> "content-type",
    "Access-Control-Allow-Methods" -> "GET, POST, OPTIONS, PUT, DELETE, PATCH")

  override def apply(nextFilter: RequestHeader => Future[Result])(requestHeader: RequestHeader): Future[Result] = {
    nextFilter(requestHeader).map { result =>
      result.withHeaders(corsHeaders:_*)
    }
  }
}

object AccessLogFilter extends Filter {

  override def apply(nextFilter: RequestHeader => Future[Result])(rq: RequestHeader): Future[Result] = {
    val ts = new Instant
    val fmt = DateTimeFormat.forPattern("dd/MMM/yyyy:hh:mm:dd Z")
    val responseLength = Iteratee.fold(0) { (length, bytes: Array[Byte]) => length + bytes.length }
    for {
      result <- nextFilter(rq)
      counter <- result.body(responseLength)
      size <- counter.run
    } yield {
      val ip = rq.remoteAddress
      val time = fmt.print(ts)
      val code = result.header.status
      val req = s"""$ip - - [$time] "${rq.method} ${rq.uri} ${rq.version}" $code $size"""
      play.Logger.of("accesslog").info(req)
      result
    }
  }
}

package object global {

  object Address {
    val p = Promise[(Boolean, String)]()

    lazy val secure: Boolean = Await.result(p.future, Duration(2, TimeUnit.SECONDS))._1
    lazy val hostname: String = Await.result(p.future, Duration(2, TimeUnit.SECONDS))._2
  }

}

object Global extends WithFilters(CorsFilter, AccessLogFilter) {

  import global.Address.p

  override def onRequestReceived(request: RequestHeader): (RequestHeader, Handler) = {
    if (!p.isCompleted) {
      p.success((request.secure, request.host))
    }
    super.onRequestReceived(request)
  }

}
