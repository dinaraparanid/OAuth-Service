package com.paranid5.auth_service

import cats.data.Kleisli
import cats.effect.unsafe.IORuntime
import cats.effect.{ExitCode, IO, IOApp, Resource}
import cats.syntax.all.*

import com.comcast.ip4s.{ipv4, port}

import com.paranid5.auth_service.di.AppModule
import com.paranid5.auth_service.routing.auth.authRouter
import com.paranid5.auth_service.routing.oauth.oauthRouter
import com.paranid5.auth_service.token.generateToken

import org.http4s.dsl.io.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.{Request, Response}

object App extends IOApp:
  private val appModule: Resource[IO, Either[Throwable, AppModule]] =
    AppModule(IORuntime.global)

  override def run(args: List[String]): IO[ExitCode] =
    appModule use:
      _.fold(
        fa = _ ⇒ IO(ExitCode.Error),
        fb = runServer
      )

  private def runServer(appModule: AppModule): IO[ExitCode] =
    EmberServerBuilder
      .default[IO]
      .withHost(ipv4"0.0.0.0")
      .withPort(port"4000")
      .withHttpApp(authService)
      .build
      .use(_ => IO.never)
      .as(ExitCode.Success)

  private def authService: Kleisli[IO, Request[IO], Response[IO]] =
    (authRouter <+> oauthRouter).orNotFound

  private def sendToken(name: String): IO[Response[IO]] =
    Ok(generateToken(name).map(_.getOrElse("")))
