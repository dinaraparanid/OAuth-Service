package com.paranid5.auth_service.routing.oauth

import cats.data.Reader
import cats.effect.IO

import com.paranid5.auth_service.data.oauth.token.entity.TokenEntity
import com.paranid5.auth_service.data.oauth.token.error.InvalidTokenReason
import com.paranid5.auth_service.routing.*
import com.paranid5.auth_service.routing.oauth.entity.RefreshRequest
import com.paranid5.auth_service.routing.oauth.response.accessTokenRefreshed
import com.paranid5.auth_service.utills.extensions.ApplicativeEitherOps.foldTraverseR

import doobie.free.connection.ConnectionIO

import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import org.http4s.dsl.io.*
import org.http4s.{Request, Response}

/**
 * Validates access token for authorization for platform.
 * Redirects either to provided callback url or the [[DefaultRedirect]]
 *
 * ==Route==
 * POST /oauth/refresh?client_id=123&client_secret=secret
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
 * 4. [[Created]] with new access token:
 * {{{
 *   {
 *     "access_token":  {
 *       "token_id":     1
 *       "client_id":    123,
 *       "app_id":       null,     // always null
 *       "value":        "abcdef",
 *       "life_seconds": 100,
 *       "created_at":   100,      // time since January 1, 1970 UTC
 *       "status":       "access"
 *     }
 *   }
 * }}}
 */

private def onPlatformRefresh(
  query:        Request[IO],
  clientId:     Long,
  clientSecret: String,
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
      retrievedTokenRes: Either[InvalidTokenReason, TokenEntity]
    ): ConnectionIO[IO[Response[IO]]] =
      retrievedTokenRes.foldTraverseR(
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
          fb = _ ⇒ generateAccessToken(token)
        )
      yield response

    def generateAccessToken(refreshToken: TokenEntity): ConnectionIO[IO[Response[IO]]] =
      for
        _              ← oauthRepository.deletePlatformAccessTokenWithScopes(clientId)
        accessTokenRes ← oauthRepository.newPlatformAccessToken(refreshToken)
      yield accessTokenRes.fold(
        fa = x ⇒ somethingWentWrong,
        fb = t ⇒ accessTokenRefreshed(t.entity)
      )

    processRequest(query.attemptAs[RefreshRequest])(retrieveRefreshToken) run appModule
