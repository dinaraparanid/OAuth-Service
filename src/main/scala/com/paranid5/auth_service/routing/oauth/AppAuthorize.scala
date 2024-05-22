package com.paranid5.auth_service.routing.oauth

import cats.data.Reader
import cats.effect.IO
import cats.syntax.all.*
import com.paranid5.auth_service.data.oauth.client.entity.AppEntity
import com.paranid5.auth_service.data.oauth.token.entity.AccessToken
import com.paranid5.auth_service.routing.*
import com.paranid5.auth_service.routing.oauth.entity.AuthorizeRequest
import doobie.free.connection.ConnectionIO
import doobie.syntax.all.*
import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import org.http4s.{DecodeResult, Request, Response}

/**
 * Validates access token for authorization for client app.
 * Redirects either to provided callback url, app's callback url or the [[DefaultRedirect]]
 *
 * ==Route==
 * POST /oauth/authorize?client_id=123&app_id=123&app_secret=secret&redirect_url=https://...
 *
 * ==Body==
 * {{{
 *   {
 *     "token": "abcdef"
 *   }
 * }}}
 *
 * ==Response==
 * 1. [[BadRequest]] - "Invalid body"
 *
 * 2. [[NotFound]] - "App was not found"
 *
 * 3. [[NotFound]] - "Token was not found"
 *
 * 4. [[Forbidden]] - "Token has expired"
 *
 * 5. [[Found]] with either provided callback url,
 * app's callback url or with [[DefaultRedirect]]
 */

private def onAppAuthorize(
  query:       Request[IO],
  clientId:    Long,
  appId:       Long,
  appSecret:   String,
  redirectUrl: Option[String]
): AppHttpResponse =
  Reader: appModule ⇒
    val oauthRepository = appModule.oauthModule.oauthRepository

    def retrieveApp(request: AuthorizeRequest): ConnectionIO[IO[Response[IO]]] =
      for
        appOpt   ← oauthRepository.getApp(appId, appSecret)
        response ← processApp(request.token, appOpt)
      yield response

    def processApp(
      requestToken: String,
      appOpt:       Option[AppEntity]
    ): ConnectionIO[IO[Response[IO]]] =
      appOpt
        .toRight(())
        .map(retrieveToken(requestToken, _))
        .sequence
        .map(_ getOrElse appNotFound)

    def retrieveToken(
      requestToken: String,
      app:          AppEntity
    ): ConnectionIO[IO[Response[IO]]] =
      for
        tokenOpt    ← oauthRepository.getAppAccessToken(clientId, appId, appSecret)
        callbackUrl = app.callbackUrl getOrElse (redirectUrl getOrElse DefaultRedirect)
        response    ← processToken(callbackUrl, requestToken, tokenOpt)
      yield response

    def processToken(
      callbackUrl:       String,
      requestToken:      String,
      retrievedTokenOpt: Option[AccessToken]
    ): ConnectionIO[IO[Response[IO]]] =
      retrievedTokenOpt
        .toRight(())
        .map(validateToken(callbackUrl, _))
        .sequence
        .map(_ getOrElse tokenNotFound)

    def validateToken(
      callbackUrl: String,
      token:       AccessToken
    ): ConnectionIO[IO[Response[IO]]] =
      for isValid ← oauthRepository.isTokenValid(
        clientId   = token.entity.clientId,
        tokenValue = token.entity.value
      ) yield isValid.fold(
        fa = invalidToken,
        fb = _ ⇒ redirectToCallbackUrl(callbackUrl)
      )

    processRequest(query.attemptAs[AuthorizeRequest])(retrieveApp) run appModule
