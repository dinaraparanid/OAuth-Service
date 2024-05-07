package com.paranid5.auth_service.routing.oauth

import com.paranid5.auth_service.routing.*
import cats.effect.IO
import org.http4s.{HttpRoutes, Request, Response}
import org.http4s.dsl.io.*

def oauthRouter: HttpRoutes[IO] =
  HttpRoutes
    .of[IO]:
      case query @ POST → (Root / "authorize") // принимает auth JWT токен в body, редиректит на страницу
        :? ClientIdParamMatcher(clientId)
        +& AppIdParamMatcher(appId)
        +& AppSecretParamMatcher(appSecret)
        +& RedirectUrlParamMatcher(redirectUrl) ⇒
        onAuthorize(query, clientId, appId, appSecret, redirectUrl)

      case query @ POST → (Root / "token") // создает auth и refresh токены, редиректит на что-то
        :? ClientIdParamMatcher(clientId)
        +& ClientSecretParamMatcher(clientSecret)
        +& AppIdParamMatcher(appId)
        +& AppSecretParamMatcher(appSecret)
        +& RedirectUrlParamMatcher(redirectUrl) ⇒
        onToken(query, clientId, clientSecret, appId, appSecret, redirectUrl)

      case query @ POST → (Root / "refresh") // принимает refresh токен в body, возвращает новый auth токен
        :? ClientIdParamMatcher(clientId)
        +& ClientSecretParamMatcher(clientSecret)
        +& AppIdParamMatcher(appId)
        +& AppSecretParamMatcher(appSecret) ⇒
        onRefresh(query, clientId, clientSecret, appId, appSecret)

private def onAuthorize(
  query:       Request[IO],
  clientId:    Long,
  appId:       Long,
  appSecret:   String,
  redirectUrl: Option[String]
): IO[Response[IO]] =
  Found(f"Auth $redirectUrl")

private def onToken(
  query:        Request[IO],
  clientId:     Long,
  clientSecret: String,
  appId:        Long,
  appSecret:    String,
  redirectUrl:  Option[String]
): IO[Response[IO]] =
  Found(f"Tokens $redirectUrl")

private def onRefresh(
  query:        Request[IO],
  clientId:     Long,
  clientSecret: String,
  appId:        Long,
  appSecret:    String,
): IO[Response[IO]] =
  Ok("Refresh token")
