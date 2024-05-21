package com.paranid5.auth_service.routing.oauth

import cats.data.Reader
import cats.effect.IO

import com.paranid5.auth_service.data.oauth.token.entity.TokenEntity
import com.paranid5.auth_service.data.oauth.token.error.InvalidTokenReason
import com.paranid5.auth_service.routing.oauth.entity.RefreshRequest
import com.paranid5.auth_service.routing.oauth.response.accessTokenRefreshed
import com.paranid5.auth_service.routing.*

import doobie.syntax.all.*

import org.http4s.dsl.io.*
import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import org.http4s.{DecodeResult, Request, Response}

/**
 * Validates access token for authorization for platform.
 * Redirects either to provided callback url or the [[DefaultRedirect]]
 *
 * ==Route==
 * POST /oauth/refresh?client_id=123&client_secret=secret&app_id=234&app_secret=secret&redirect_url=https://...
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
 * 2. [[NotFound]] - "Token was not found"
 *
 * 3. [[Forbidden]] - "Token has expired"
 *
 * 4. [[NotFound]] - "App was not found"
 *
 * 5. [[Created]] with new access token:
 * {{{
 *   {
 *     "access_token":  {
 *       "token_id":     1,
 *       "client_id":    123,
 *       "app_id":       234,
 *       "value":        "abcdef",
 *       "life_seconds": 100,
 *       "created_at":   100,      // time since January 1, 1970 UTC
 *       "status":       "access"
 *     }
 *   }
 * }}}
 */

private def onAppRefresh(
  query:        Request[IO],
  clientId:     Long,
  clientSecret: String,
  appId:        Long,
  appSecret:    String,
): AppHttpResponse =
  Reader: appModule ⇒
    val oauthRepository = appModule.oauthModule.oauthRepository

    def processRequest(requestRes: DecodeResult[IO, RefreshRequest]): IO[Response[IO]] =
      for
        responseIO ← requestRes.fold(_ ⇒ invalidBody, retrieveRefreshToken)
        response   ← responseIO
      yield response

    def retrieveRefreshToken(request: RefreshRequest): IO[Response[IO]] =
      for
        tokenRes ← oauthRepository.findToken(clientId, request.token).transact(appModule.transcactor)
        response ← processRefreshToken(request.token, tokenRes)
      yield response

    def processRefreshToken(
      requestToken:      String,
      retrievedTokenOpt: Either[InvalidTokenReason, TokenEntity]
    ): IO[Response[IO]] =
      retrievedTokenOpt.fold(
        fa = _ ⇒ tokenNotFound,
        fb = token ⇒ validateRefreshToken(token)
      )

    def validateRefreshToken(token: TokenEntity): IO[Response[IO]] =
      for
        isValid ← oauthRepository.isTokenValid(
          clientId   = token.clientId,
          tokenValue = token.value
        ).transact(appModule.transcactor)

        response ← isValid.fold(
          fa = invalidToken,
          fb = _ ⇒ retrieveApp(token)
        )
      yield response

    def retrieveApp(refreshToken: TokenEntity): IO[Response[IO]] =
      for
        appOpt ← oauthRepository
          .getApp(appId, appSecret)
          .transact(appModule.transcactor)

        response ← appOpt.fold(
          ifEmpty = appNotFound)(
          f       = _ ⇒ generateAccessToken(refreshToken, appId)
        )
      yield response

    def generateAccessToken(
      refreshToken: TokenEntity,
      appId:        Long,
    ): IO[Response[IO]] =
      for
        accessTokenRes ← oauthRepository
          .newAppAccessToken(refreshToken, appId)
          .transact(appModule.transcactor)

        response ← accessTokenRes.fold(
          fa = _ ⇒ somethingWentWrong,
          fb = t ⇒ accessTokenRefreshed(t.entity)
        )
      yield response

    processRequest(query.attemptAs[RefreshRequest])
