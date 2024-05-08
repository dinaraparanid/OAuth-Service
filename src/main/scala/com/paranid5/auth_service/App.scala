package com.paranid5.auth_service

import cats.data.{Kleisli, Reader}
import cats.effect.{ExitCode, IO, IOApp}
import cats.syntax.all.*

import com.comcast.ip4s.{ipv4, port}

import com.paranid5.auth_service.di.{AppModule, AppDependencies}
import com.paranid5.auth_service.routing.auth.authRouter
import com.paranid5.auth_service.routing.oauth.oauthRouter

import org.http4s.ember.server.EmberServerBuilder
import org.http4s.{Request, Response}

object App extends IOApp:
  override def run(args: List[String]): IO[ExitCode] =
    AppModule(runServer() run _) map:
      _.fold(fa = _ ⇒ ExitCode.Error, fb = _ ⇒ ExitCode.Success)

  private def runServer(): AppDependencies[IO[ExitCode]] =
    Reader: appModule ⇒
      EmberServerBuilder
        .default[IO]
        .withHost(ipv4"0.0.0.0")
        .withPort(port"4000")
        .withHttpApp(authService run appModule)
        .build
        .use(_ => IO.never)
        .as(ExitCode.Success)

  private def authService: AppDependencies[Kleisli[IO, Request[IO], Response[IO]]] =
    for
      auth  ← authRouter
      oauth ← oauthRouter
    yield (auth <+> oauth).orNotFound
