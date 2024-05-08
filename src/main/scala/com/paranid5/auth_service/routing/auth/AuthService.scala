package com.paranid5.auth_service.routing.auth

import cats.data.Reader
import cats.effect.IO

import com.paranid5.auth_service.routing.*
import com.paranid5.auth_service.routing.auth.entity.LoginPasswordEntity

import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import org.http4s.dsl.io.*
import org.http4s.{HttpRoutes, Request, Response}

def authService: AppRoutes =
  Reader: appModule ⇒
    HttpRoutes
      .of[IO]:
        case query @ POST → (Root / "sign_up") ⇒ // принмает логин и пароль в body, возвращает client_id и client_secret
          onSignUp(query) run appModule

        case query @ POST → (Root / "sing_in") ⇒ // принмает логин и пароль в body, возвращает client_id и client_secret
          onSignIn(query) run appModule

        case query @ POST → (Root / "sign_out") ⇒ // логин
          onSignOut(query) run appModule

private def onSignUp(query: Request[IO]): AppHttpResponse =
  Reader: appModule ⇒
    for
      loginPassword ← query.as[LoginPasswordEntity]
      resp          ← Ok(s"Sign Up $loginPassword")
    yield resp

private def onSignIn(query: Request[IO]): AppHttpResponse =
  Reader: appModule ⇒
    Ok("Sign in")

private def onSignOut(query: Request[IO]): AppHttpResponse =
  Reader: appModule ⇒
    Ok("Sign out")