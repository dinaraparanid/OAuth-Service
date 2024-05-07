package com.paranid5.auth_service.routing.auth

import cats.effect.IO
import org.http4s.{HttpRoutes, Request, Response}
import org.http4s.dsl.io.*

def authRouter: HttpRoutes[IO] =
  HttpRoutes
    .of[IO]:
      case query @ POST → (Root / "sign_up") ⇒ // принмает логин и пароль в body, возвращает client_id и client_secret
         onSignUp(query)

      case query @ POST → (Root / "sing_in") ⇒ // принмает логин и пароль в body, возвращает client_id и client_secret
        onSignIn(query)

      case query @ POST → (Root / "sign_out") ⇒ // логин
        onSignOut(query)

private def onSignUp(query: Request[IO]): IO[Response[IO]] =
  Ok("Sign Up")

private def onSignIn(query: Request[IO]): IO[Response[IO]] =
  Ok("Sign in")

private def onSignOut(query: Request[IO]): IO[Response[IO]] =
  Ok("Sign out")