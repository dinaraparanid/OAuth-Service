package com.paranid5.auth_service.oauth

import com.paranid5.auth_service.entity.User
import cats.effect.IO
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.http4s.dsl.io.*

private object ClientIdParamMatcher extends QueryParamDecoderMatcher[Int]("client_id")
private object ClientSecretParamMatcher extends QueryParamDecoderMatcher[String]("client_secret")
private object RedirectUrlParamMatcher extends OptionalQueryParamDecoderMatcher[String]("redirect_url")
private object AuthCodeParamMatcher extends QueryParamDecoderMatcher[String]("auth_code")

def oauthRouter =
  HttpRoutes
    .of[IO]:
      case GET → (Root / "authorize") // принимает auth JWT токен в body, редиректит на страницу
        :? ClientIdParamMatcher(clientId)
        +& RedirectUrlParamMatcher(redirectUrl) ⇒ Found(f"Auth $redirectUrl")

      case POST → (Root / "token") // создает auth и refresh токены, редиректит на что-то
        :? ClientIdParamMatcher(clientId)
        +& ClientSecretParamMatcher(clientSecret)
        +& AuthCodeParamMatcher(code)
        +& RedirectUrlParamMatcher(redirectUrl) ⇒ Found(f"Tokens $redirectUrl")

      case POST → (Root / "refresh") // принимает refresh токен в body, возвращает новый auth токен
        :? ClientIdParamMatcher(clientId)
        +& ClientSecretParamMatcher(clientSecret) ⇒ Ok("Refresh token")
