package com.paranid5.auth_service.routing.oauth

import cats.data.Reader
import cats.effect.IO

import com.paranid5.auth_service.data.oauth.token.entity.TokenEntity
import com.paranid5.auth_service.data.oauth.token.error.InvalidTokenReason
import com.paranid5.auth_service.routing.*
import com.paranid5.auth_service.routing.oauth.entity.RefreshRequest
import com.paranid5.auth_service.routing.oauth.response.accessTokenRefreshed

import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import org.http4s.dsl.io.*
import org.http4s.{DecodeResult, Request, Response}

/**
 * Validates access token for authorization for platform.
 * Redirects either to provided callback url or the [[DefaultRedirect]]
 *
 * ==Route==
 * POST /oauth/authorize?client_id=123&redirect_url=https://...
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
 *       "value":        "abcdef", // 45-th length string
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

    def processRequest(requestRes: DecodeResult[IO, RefreshRequest]): IO[Response[IO]] =
      for
        responseIO ← requestRes.fold(_ ⇒ invalidBody, retrieveRefreshToken)
        response   ← responseIO
      yield response

    def retrieveRefreshToken(request: RefreshRequest): IO[Response[IO]] =
      for
        tokenRes ← oauthRepository.findToken(clientId, request.token)
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
        )

        response ← isValid.fold(
          fa = invalidToken,
          fb = _ ⇒ generateAccessToken(token)
        )
      yield response

    def generateAccessToken(refreshToken: TokenEntity): IO[Response[IO]] =
      for
        _              ← oauthRepository.deletePlatformAccessTokenWithScopes(clientId)
        accessTokenRes ← oauthRepository.newPlatformAccessToken(refreshToken)
        response ← accessTokenRes.fold(
          fa = x ⇒ somethingWentWrong,
          fb = t ⇒ accessTokenRefreshed(t.entity)
        )
      yield response

    processRequest(query.attemptAs[RefreshRequest])
