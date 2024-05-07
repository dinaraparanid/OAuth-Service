package com.paranid5.auth_service.routing.app

import com.paranid5.auth_service.routing.*

import cats.effect.IO

import org.http4s.{HttpRoutes, Request, Response}
import org.http4s.dsl.io.*

def appRouter: HttpRoutes[IO] =
  HttpRoutes
    .of[IO]:
      case query @ POST → Root ⇒
        onCreate(query)

      case query @ GET → Root :? AppIdParamMatcher(appId) ⇒
        onFind(query)

      case query @ PATCH → Root
        :? ClientIdParamMatcher(clientId)
        +& AppIdParamMatcher(appId)
        +& AppSecretParamMatcher(appSecret) ⇒
        onUpdate(query)

      case query @ DELETE → Root
        :? ClientIdParamMatcher(clientId)
        +& AppIdParamMatcher(appId)
        +& AppSecretParamMatcher(appSecret) ⇒
        onDelete(query)

private def onCreate(query: Request[IO]): IO[Response[IO]] =
  Created("App created")

private def onFind(query: Request[IO]): IO[Response[IO]] =
  Ok("App found")

private def onUpdate(query: Request[IO]): IO[Response[IO]] =
  Ok("App updated")

private def onDelete(query: Request[IO]): IO[Response[IO]] =
  Ok("App Deleted")
