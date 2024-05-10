package com.paranid5.auth_service.routing.auth

import cats.data.Reader
import cats.effect.IO

import com.paranid5.auth_service.routing.*

import org.http4s.dsl.io.*
import org.http4s.server.middleware.CORS
import org.http4s.{HttpRoutes, Request, Response}

def authService: AppRoutes =
  Reader: appModule ⇒
    CORS.policy.withAllowOriginAll:
      HttpRoutes.of[IO]: // TODO: проверка email-а + восстановление пароля
        case query @ POST → (Root / "sign_up") ⇒ onSignUp(query) run appModule
        case query @ POST → (Root / "sign_in") ⇒ onSignIn(query) run appModule
