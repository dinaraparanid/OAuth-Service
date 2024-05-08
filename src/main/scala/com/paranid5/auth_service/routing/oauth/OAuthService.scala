package com.paranid5.auth_service.routing.oauth

import cats.data.Reader
import cats.effect.IO

import com.paranid5.auth_service.di.{AppDependencies, AppModule}
import com.paranid5.auth_service.routing.*

import org.http4s.{HttpRoutes, Request, Response}
import org.http4s.dsl.io.*

def oauthService: AppRoutes =
  Reader: appModule ⇒
    HttpRoutes
      .of[IO]:
        case query @ POST → (Root / "authorize") // принимает auth JWT токен в body, редиректит на страницу
          :? ClientIdParamMatcher(clientId)
          +& AppIdParamMatcher(appId)
          +& AppSecretParamMatcher(appSecret)
          +& RedirectUrlParamMatcher(redirectUrl) ⇒
          onAuthorize(query, clientId, appId, appSecret, redirectUrl) run appModule

        case query @ POST → (Root / "token") // создает auth и refresh токены, редиректит на что-то
          :? ClientIdParamMatcher(clientId)
          +& ClientSecretParamMatcher(clientSecret)
          +& AppIdParamMatcher(appId)
          +& AppSecretParamMatcher(appSecret)
          +& RedirectUrlParamMatcher(redirectUrl) ⇒
          onToken(query, clientId, clientSecret, appId, appSecret, redirectUrl) run appModule

        case query @ POST → (Root / "refresh") // принимает refresh токен в body, возвращает новый auth токен
          :? ClientIdParamMatcher(clientId)
          +& ClientSecretParamMatcher(clientSecret)
          +& AppIdParamMatcher(appId)
          +& AppSecretParamMatcher(appSecret) ⇒
          onRefresh(query, clientId, clientSecret, appId, appSecret) run appModule

private def onAuthorize(
  query:       Request[IO],
  clientId:    Long,
  appId:       Long,
  appSecret:   String,
  redirectUrl: Option[String]
): AppHttpResponse =
  Reader: appModule ⇒
    Found(f"Auth $redirectUrl")

private def onToken(
  query:        Request[IO],
  clientId:     Long,
  clientSecret: String,
  appId:        Long,
  appSecret:    String,
  redirectUrl:  Option[String]
): AppHttpResponse =
  Reader: appModule ⇒
    Found(f"Tokens $redirectUrl")

private def onRefresh(
  query:        Request[IO],
  clientId:     Long,
  clientSecret: String,
  appId:        Long,
  appSecret:    String,
): AppHttpResponse =
  Reader: appModule ⇒
    Ok("Refresh token")
