package com.paranid5.auth_service.auth

import cats.effect.IO
import org.http4s.HttpRoutes
import org.http4s.dsl.io.*

def authRouter: HttpRoutes[IO] =
  HttpRoutes
    .of[IO]:
      case POST → (Root / "sign_up") ⇒ // принмает логин и пароль в body, возвращает client_id и client_secret
         Ok("Sign Up")

      case POST → (Root / "sing_in") ⇒ // принмает логин и пароль в body, возвращает client_id и client_secret
        Ok("Sign in")

      case POST → (Root / "sign_out") ⇒ // логин
        Ok("Sign out")
