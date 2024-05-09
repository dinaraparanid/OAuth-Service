package com.paranid5.auth_service.routing.app

import cats.data.Reader
import cats.effect.IO

import com.paranid5.auth_service.routing.*

import org.http4s.{HttpRoutes, Request, Response}
import org.http4s.dsl.io.*

def manageAppService: AppRoutes =
  Reader: appModule ⇒
    HttpRoutes.of[IO]:
      case query @ POST → Root ⇒
        onCreate(query) run appModule

      case query @ GET → Root :? AppIdParamMatcher(appId) ⇒
        onFind(query) run appModule

      case query @ PATCH → Root
        :? ClientIdParamMatcher(clientId)
        +& AppIdParamMatcher(appId)
        +& AppSecretParamMatcher(appSecret) ⇒
        onUpdate(query) run appModule

      case query @ DELETE → Root
        :? ClientIdParamMatcher(clientId)
        +& AppIdParamMatcher(appId)
        +& AppSecretParamMatcher(appSecret) ⇒
        onDelete(query) run appModule

private def onCreate(query: Request[IO]): AppHttpResponse =
  Reader: appModule ⇒
    Created("App created")

private def onFind(query: Request[IO]): AppHttpResponse =
  Reader: appModule ⇒
    Ok("App found")

private def onUpdate(query: Request[IO]): AppHttpResponse =
  Reader: appModule ⇒
    Ok("App updated")

private def onDelete(query: Request[IO]): AppHttpResponse =
  Reader: appModule ⇒
    Ok("App Deleted")