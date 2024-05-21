package com.paranid5.auth_service

import cats.data.{Kleisli, Reader}
import cats.effect.{ExitCode, IO, IOApp}

import com.comcast.ip4s.{ipv4, port}

import com.paranid5.auth_service.di.{AppDependencies, AppModule}
import com.paranid5.auth_service.routing.app.manageAppRoutes
import com.paranid5.auth_service.routing.auth.authRoutes
import com.paranid5.auth_service.routing.oauth.oauthRoutes

import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router
import org.http4s.{Request, Response}

object App extends IOApp:
  override def run(args: List[String]): IO[ExitCode] =
    AppModule(runServer() run _) map:
      _.fold(fa = _ ⇒ ExitCode.Error, fb = _ ⇒ ExitCode.Success)

  private def runServer(): AppDependencies[IO[ExitCode]] =
    Reader: appModule ⇒
      def impl: IO[ExitCode] =
        EmberServerBuilder
          .default[IO]
          .withHost(ipv4"0.0.0.0")
          .withPort(port"8080")
          .withHttpApp(appService run appModule)
          .build
          .use(_ => IO.never)
          .as(ExitCode.Success)

      val authRepository = appModule.userModule.userRepository
      val oauthRepository = appModule.oauthModule.oauthRepository

      for
        _   ← authRepository.createTablesTransact()
        _   ← oauthRepository.createTables()
        res ← impl
      yield res

  private def appService: AppDependencies[Kleisli[IO, Request[IO], Response[IO]]] =
    for
      auth      ← authRoutes
      oauth     ← oauthRoutes
      manageApp ← manageAppRoutes
    yield Router(
      "/auth"  -> auth,
      "/oauth" -> oauth,
      "/app"   -> manageApp
    ).orNotFound
