package com.paranid5.auth_service.routing.oauth

import cats.data.Reader
import cats.effect.IO

import com.paranid5.auth_service.data.oauth.client.entity.{AppEntity, ClientEntity}
import com.paranid5.auth_service.data.oauth.token.entity.TokenEntity
import com.paranid5.auth_service.data.user.entity.User
import com.paranid5.auth_service.routing.*
import com.paranid5.auth_service.routing.auth.entity.{SignInRequest, matches}
import com.paranid5.auth_service.routing.oauth.entity.TokenResponse
import com.paranid5.auth_service.routing.oauth.response.tokensGenerated

import io.circe.syntax.*

import org.http4s.circe.CirceEntityCodec.{circeEntityDecoder, circeEntityEncoder}
import org.http4s.dsl.io.*
import org.http4s.{DecodeResult, Request, Response}

/**
 * Updates both refresh and access tokens for the platform only
 * by removing old tokens and generating new ones.
 *
 * ==Route==
 * POST /oauth/token?client_id=123&client_secret=secret&redirect_url=https://...
 *
 * ==Response==
 * 1. [[BadRequest]] - "Invalid body"
 *
 * 2. [[NotFound]] - "Client was not found"
 *
 * 3. [[Found]] with both access and refresh tokens and redirect url:
 * {{{
 *   {
 *     "access_token":  {
 *       "client_id":    123,
 *       "app_id":       null,     // always null
 *       "value":        "abcdef", // 45-th length string
 *       "life_seconds": 100,
 *       "created_at":   100,      // time since January 1, 1970 UTC
 *       "status":       "access"
 *     },
 *     "refresh_token":  {
 *       "client_id":    123,
 *       "app_id":       null,     // always null
 *       "value":        "abcdef", // 45-th length string
 *       "life_seconds": 100,
 *       "created_at":   100,      // time since January 1, 1970 UTC
 *       "status":       "refresh"
 *     },
 *     "redirect_url": "https://..."
 *   }
 * }}}
 */

private def onPlatformToken(
  clientId:     Long,
  clientSecret: String,
  redirectUrl:  Option[String],
): AppHttpResponse =
  Reader: appModule ⇒
    val userRepository  = appModule.userModule.userRepository
    val oauthRepository = appModule.oauthModule.oauthRepository

    def retrieveCredentials: IO[Response[IO]] =
      for
        clientOpt ← oauthRepository.findClient(clientId, clientSecret)
        response  ← clientOpt.fold(
          ifEmpty = clientNotFound)(
          f       = removeOldTokens
        )
      yield response

    def removeOldTokens(client: ClientEntity): IO[Response[IO]] =
      for
        _        ← oauthRepository.deleteRefreshToken(client.clientId)
        _        ← oauthRepository.deletePlatformAccessTokenWithScopes(client.clientId)
        response ← generateRefreshToken(client)
      yield response

    def generateRefreshToken(client: ClientEntity): IO[Response[IO]] =
      for
        refreshTokenRes ← oauthRepository.newRefreshToken(
          clientId     = client.clientId,
          clientSecret = client.clientSecret
        )

        response ← refreshTokenRes.fold(
          fa = _ ⇒ somethingWentWrong,
          fb = generateAccessToken
        )
      yield response

    def generateAccessToken(refreshToken: TokenEntity): IO[Response[IO]] =
      for
        accessTokenRes ← oauthRepository.newPlatformAccessToken(refreshToken)

        response ← accessTokenRes.fold(
          fa = _ ⇒ somethingWentWrong,
          fb = accessToken ⇒
            tokensGenerated(
              accessToken  = accessToken.entity,
              refreshToken = refreshToken,
              redirectUrl  = redirectUrl getOrElse DefaultRedirect
            )
        )
      yield response

    retrieveCredentials
