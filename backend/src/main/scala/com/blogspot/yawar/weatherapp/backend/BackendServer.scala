package com.blogspot.yawar.weatherapp.backend

import java.util.concurrent.{ ExecutorService, Executors }
import org.http4s.server.{ Server, ServerApp }
import org.http4s.server.blaze.BlazeBuilder
import org.http4s.server.middleware._
import scala.util.Properties.envOrNone
import scalaz.concurrent.Task

object BackendServer extends ServerApp {
  val port: Int = envOrNone("HTTP_PORT") map (_.toInt) getOrElse 8080
  val ip: String = "0.0.0.0"
  val pool: ExecutorService = Executors.newCachedThreadPool()

  override def server(args: List[String]): Task[Server] =
    BlazeBuilder
      .mountService(CORS(BackendService.service))
      .withServiceExecutor(pool)
      .start
}
