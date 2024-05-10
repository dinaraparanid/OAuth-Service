package com.paranid5.auth_service.routing.oauth

import cats.data.Reader
import cats.effect.IO

import com.paranid5.auth_service.routing.*

import org.http4s.dsl.io.*
import org.http4s.server.middleware.CORS
import org.http4s.{HttpRoutes, Request, Response}

def oauthService: AppRoutes =
  Reader: appModule ⇒
    CORS.policy.withAllowOriginAll:
      HttpRoutes.of[IO]:
        case query @ POST → (Root / "authorize")
          :? ClientIdParamMatcher(clientId)
          +& RedirectUrlParamMatcher(redirectUrl) ⇒
          onPlatformAuthorize(query, clientId, redirectUrl) run appModule

        case query @ POST → (Root / "authorize")
          :? ClientIdParamMatcher(clientId)
          +& AppIdParamMatcher(appId)
          +& AppSecretParamMatcher(appSecret)
          +& RedirectUrlParamMatcher(redirectUrl) ⇒
          onAppAuthorize(query, clientId, appId, appSecret, redirectUrl) run appModule

        // TODO: По-хорошему, здесь должен быть auth_code, получаемый из sign_up/sign_in
        case POST → (Root / "token")
          :? ClientIdParamMatcher(clientId)
          +& ClientSecretParamMatcher(clientSecret)
          +& RedirectUrlParamMatcher(redirectUrl) ⇒
          onPlatformToken(clientId, clientSecret, redirectUrl) run appModule

        // TODO: По-хорошему, здесь должен быть auth_code, получаемый из sign_up/sign_in
        case POST → (Root / "token")
          :? ClientIdParamMatcher(clientId)
          +& ClientSecretParamMatcher(clientSecret)
          +& AppIdParamMatcher(appId)
          +& AppSecretParamMatcher(appSecret)
          +& RedirectUrlParamMatcher(redirectUrl) ⇒
          onAppToken(clientId, clientSecret, appId, appSecret, redirectUrl) run appModule

        case query @ POST → (Root / "refresh")
          :? ClientIdParamMatcher(clientId)
          +& ClientSecretParamMatcher(clientSecret) ⇒
          onPlatformRefresh(query, clientId, clientSecret) run appModule

        case query @ POST → (Root / "refresh")
          :? ClientIdParamMatcher(clientId)
          +& ClientSecretParamMatcher(clientSecret)
          +& AppIdParamMatcher(appId)
          +& AppSecretParamMatcher(appSecret) ⇒
          onAppRefresh(query, clientId, clientSecret, appId, appSecret) run appModule

        case GET → (Root / "user") :? AccessTokenParamMatcher(accessToken) ⇒
          onFindUser(accessToken) run appModule

