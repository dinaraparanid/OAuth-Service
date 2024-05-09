package com.paranid5.auth_service.routing.oauth

import cats.data.Reader
import cats.effect.IO

import com.paranid5.auth_service.data.oauth.client.entity.AppEntity
import com.paranid5.auth_service.data.oauth.token.entity.AccessToken
import com.paranid5.auth_service.data.oauth.token.error
import com.paranid5.auth_service.data.oauth.token.error.InvalidTokenReason
import com.paranid5.auth_service.routing.AppHttpResponse
import com.paranid5.auth_service.routing.oauth.entity.AuthorizeRequest

import org.http4s.circe.CirceEntityCodec.{circeEntityDecoder, circeEntityEncoder}
import org.http4s.dsl.io.*
import org.http4s.{DecodeResult, Request, Response}

private val DefaultRedirect = "http://0.0.0.0:4000/"

private def invalidBody: IO[Response[IO]] =
  BadRequest("Invalid body")

private def appNotFound: IO[Response[IO]] =
  NotFound("App was not found")

private def tokenNotFound: IO[Response[IO]] =
  NotFound("Token was not found")

private def tokenExpired: IO[Response[IO]] =
  Forbidden("Token has expired")

private def somethingWentWrong: IO[Response[IO]] =
  InternalServerError("Something went wrong")

private def invalidToken: InvalidTokenReason ⇒ IO[Response[IO]] =
  case InvalidTokenReason.Expired         ⇒ tokenExpired
  case InvalidTokenReason.NotFound        ⇒ tokenNotFound
  case InvalidTokenReason.GenerationError ⇒ somethingWentWrong

private def redirectToCallbackUrl(callbackUrl: Option[String]): IO[Response[IO]] =
  Found(callbackUrl getOrElse DefaultRedirect)

/**
 * Validates access token for authorization in client app
 *
 * ==Route==
 * POST /oauth/authorize?client_id=123&app_id=123&app_secret=secret&redirect_url=https://...
 *
 * ==Body==
 * {{{
 *   {
 *     "token": "abcdef" // 45-th length string
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
 * 5. [[Found]] with app's callback url or with [[DefaultRedirect]]
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

    def processRequest(requestRes: DecodeResult[IO, AuthorizeRequest]): IO[Response[IO]] =
      for
        responseIO ← requestRes.fold(_ ⇒ invalidBody, retrieveApp)
        response   ← responseIO
      yield response

    def retrieveApp(request: AuthorizeRequest): IO[Response[IO]] =
      for
        appOpt   ← oauthRepository.getApp(appId, appSecret)
        response ← processApp(request.token, appOpt)
      yield response

    def processApp(
      requestToken: String,
      appOpt: Option[AppEntity]
    ): IO[Response[IO]] =
      for
        response ← appOpt.fold(
          ifEmpty = appNotFound)(
          f       = app ⇒ retrieveToken(requestToken, app)
        )
      yield response

    def retrieveToken(
      requestToken: String,
      app:          AppEntity
    ): IO[Response[IO]] =
      for
        tokenOpt ← oauthRepository.getAppAccessToken(clientId, appId, appSecret)
        response ← processToken(app.callbackUrl, requestToken, tokenOpt)
      yield response

    def processToken(
      callbackUrl:       Option[String],
      requestToken:      String,
      retrievedTokenOpt: Option[AccessToken]
    ): IO[Response[IO]] =
      retrievedTokenOpt.fold(
        ifEmpty = tokenNotFound)(
        f       = token ⇒ validateToken(callbackUrl, token)
      )

    def validateToken(
      callbackUrl: Option[String],
      token:       AccessToken
    ): IO[Response[IO]] =
      for
        isValid ← oauthRepository.isTokenValid(
          clientId   = token.entity.clientId,
          tokenValue = token.entity.value
        )

        response ← isValid.fold(
          fa = invalidToken,
          fb = _ ⇒ redirectToCallbackUrl(callbackUrl)
        )
      yield response

    processRequest(query.attemptAs[AuthorizeRequest])
