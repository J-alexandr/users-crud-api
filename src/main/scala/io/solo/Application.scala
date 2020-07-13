package io.solo

import cats.effect._
import org.http4s.server.blaze.BlazeServerBuilder

import scala.concurrent.ExecutionContext.global

object Application extends IOApp {

  def run(args: List[String]): IO[ExitCode] =
    BlazeServerBuilder[IO](global)
      .bindHttp(8080, "localhost")
      .withHttpApp(UserRoutes.route())
      .serve
      .compile
      .drain
      .as(ExitCode.Success)
}