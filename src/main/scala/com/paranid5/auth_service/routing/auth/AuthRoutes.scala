package com.paranid5.auth_service.routing.auth

import cats.data.Reader
import cats.effect.IO

import com.paranid5.auth_service.routing.*

import org.http4s.dsl.io.*
import org.http4s.server.middleware.CORS
import org.http4s.{HttpRoutes, Request, Response}

def authRoutes: AppRoutes =
  Reader: appModule ⇒
    CORS.policy.withAllowOriginAll:
      HttpRoutes.of[IO]: // TODO: восстановление пароля
        case query @ POST → (Root / "sign_up") ⇒ onSignUp(query) run appModule

        case query @ POST → (Root / "sign_in") ⇒ onSignIn(query) run appModule

        case query @ POST → (Root / "confirm_email") :? ConfirmCodeParamMatcher(code) ⇒
          onConfirmEmail(code) run appModule
