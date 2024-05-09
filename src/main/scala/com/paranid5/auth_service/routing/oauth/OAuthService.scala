package com.paranid5.auth_service.routing.oauth

import cats.data.Reader
import cats.effect.IO

import com.paranid5.auth_service.routing.*

import org.http4s.dsl.io.*
import org.http4s.{HttpRoutes, Request, Response}

def oauthService: AppRoutes =
  Reader: appModule ⇒
    HttpRoutes.of[IO]:
      // TODO: По-хорошему, вместо того, чтобы сразу возвращать токены,
      //       должен возвращать code для дальнейшего получения токена в /token
      case query @ POST → (Root / "sign_up") // логин + пароль, вызвращает в body client_id, client_secret, оба токена и редиректит
        :? AppIdParamMatcher(appId)
        +& AppSecretParamMatcher(appSecret)
        +& RedirectUrlParamMatcher(redirectUrl) ⇒
        Ok("sign up for app")

      case query @ POST → (Root / "sign_in") // логин + пароль, вызвращает в body client_id, client_secret, оба токена и редиректит
        :? AppIdParamMatcher(appId)
        +& AppSecretParamMatcher(appSecret)
        +& RedirectUrlParamMatcher(redirectUrl) ⇒
        onSignIn(query, appId, appSecret, redirectUrl) run appModule

      case query @ POST → (Root / "authorize") // принимает auth JWT токен в body, редиректит на страницу приложения авторизации
        :? ClientIdParamMatcher(clientId)
        +& RedirectUrlParamMatcher(redirectUrl) ⇒
        onPlatformAuthorize(query, clientId, redirectUrl) run appModule

      case query @ POST → (Root / "authorize") // принимает auth JWT токен в body, редиректит на страницу клиентского приложения
        :? ClientIdParamMatcher(clientId)
        +& AppIdParamMatcher(appId)
        +& AppSecretParamMatcher(appSecret)
        +& RedirectUrlParamMatcher(redirectUrl) ⇒
        onAppAuthorize(query, clientId, appId, appSecret, redirectUrl) run appModule

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
