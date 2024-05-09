package com.paranid5.auth_service.routing.auth

import cats.data.Reader
import cats.effect.IO

import com.paranid5.auth_service.routing.*

import org.http4s.dsl.io.*
import org.http4s.{HttpRoutes, Request, Response}

def authService: AppRoutes =
  Reader: appModule ⇒
    HttpRoutes.of[IO]:
      case query @ POST → (Root / "sign_up") ⇒ onSignUp(query) run appModule
      case query @ POST → (Root / "sing_in") ⇒ onSignIn(query) run appModule
