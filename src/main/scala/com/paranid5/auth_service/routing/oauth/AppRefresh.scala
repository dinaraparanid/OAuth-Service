package com.paranid5.auth_service.routing.oauth

import cats.data.Reader
import cats.effect.IO

import com.paranid5.auth_service.data.oauth.token.entity.TokenEntity
import com.paranid5.auth_service.data.oauth.token.error.InvalidTokenReason
import com.paranid5.auth_service.routing.*
import com.paranid5.auth_service.routing.oauth.entity.RefreshRequest
import com.paranid5.auth_service.routing.oauth.response.accessTokenRefreshed
import com.paranid5.auth_service.utills.extensions.ApplicativeEitherOps.foldTraverseR
import com.paranid5.auth_service.utills.extensions.ApplicativeOptionOps.foldTraverseR

import doobie.free.connection.ConnectionIO

import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import org.http4s.dsl.io.*
import org.http4s.{Request, Response}

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

    def retrieveRefreshToken(request: RefreshRequest): ConnectionIO[IO[Response[IO]]] =
      for
        tokenRes ← oauthRepository.findToken(clientId, request.token)
        response ← processRefreshToken(request.token, tokenRes)
      yield response

    def processRefreshToken(
      requestToken:      String,
      retrievedTokenOpt: Either[InvalidTokenReason, TokenEntity]
    ): ConnectionIO[IO[Response[IO]]] =
      retrievedTokenOpt.foldTraverseR(
        fa = _ ⇒ tokenNotFound)(
        fb = validateRefreshToken
      )

    def validateRefreshToken(token: TokenEntity): ConnectionIO[IO[Response[IO]]] =
      for
        isValid ← oauthRepository.isTokenValid(
          clientId   = token.clientId,
          tokenValue = token.value
        )

        response ← isValid.foldTraverseR(
          fa = invalidToken)(
          fb = _ ⇒ retrieveApp(token)
        )
      yield response

    def retrieveApp(refreshToken: TokenEntity): ConnectionIO[IO[Response[IO]]] =
      for
        appOpt   ← oauthRepository.getApp(appId, appSecret)
        response ← appOpt.foldTraverseR(
          ifEmpty = appNotFound)(
          f       = _ ⇒ generateAccessToken(refreshToken, appId)
        )
      yield response

    def generateAccessToken(
      refreshToken: TokenEntity,
      appId:        Long,
    ): ConnectionIO[IO[Response[IO]]] =
      for accessTokenRes ← oauthRepository.newAppAccessToken(refreshToken, appId)
        yield accessTokenRes.fold(
          fa = _ ⇒ somethingWentWrong,
          fb = t ⇒ accessTokenRefreshed(t.entity)
        )

    processRequest(query.attemptAs[RefreshRequest])(retrieveRefreshToken) run appModule
