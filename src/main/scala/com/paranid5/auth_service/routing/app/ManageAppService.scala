package com.paranid5.auth_service.routing.app

import cats.data.Reader
import cats.effect.IO

import com.paranid5.auth_service.routing.*

import org.http4s.{HttpRoutes, Request, Response}
import org.http4s.dsl.io.*
import org.http4s.server.middleware.CORS

def manageAppService: AppRoutes =
  Reader: appModule ⇒
    CORS.policy.withAllowOriginAll:
      HttpRoutes.of[IO]:
        case query @ POST → Root ⇒ onCreate(query) run appModule

        case GET → (Root / "all") :? ClientIdParamMatcher(clientId) ⇒
          onAll(clientId)    run appModule

        case GET → Root
          :? AppIdParamMatcher(appId)
          +& AppSecretParamMatcher(appSecret) ⇒
          onFind(appId, appSecret) run appModule

        case query @ PATCH → Root           ⇒ onUpdate(query) run appModule

        case query @ DELETE → Root           ⇒ onDelete(query) run appModule
