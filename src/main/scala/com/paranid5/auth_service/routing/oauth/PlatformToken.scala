package com.paranid5.auth_service.routing.oauth

import cats.data.Reader
import cats.effect.IO

import com.paranid5.auth_service.data.oauth.client.entity.ClientEntity
import com.paranid5.auth_service.data.oauth.token.entity.TokenEntity
import com.paranid5.auth_service.routing.*
import com.paranid5.auth_service.routing.oauth.response.tokensGenerated
import com.paranid5.auth_service.utills.extensions.ApplicativeEitherOps.foldTraverseR
import com.paranid5.auth_service.utills.extensions.ApplicativeOptionOps.foldTraverseR
import com.paranid5.auth_service.utills.extensions.flatTransact

import doobie.free.connection.ConnectionIO

import org.http4s.Response
import org.http4s.dsl.io.*

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
 *       "token_id":     1,
 *       "client_id":    123,
 *       "app_id":       null,     // always null
 *       "value":        "abcdef",
 *       "life_seconds": 100,
 *       "created_at":   100,      // time since January 1, 1970 UTC
 *       "status":       "access"
 *     },
 *     "refresh_token":  {
 *       "token_id":     2,
 *       "client_id":    123,
 *       "app_id":       null,     // always null
 *       "value":        "abcdef",
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

    def retrieveCredentials(): ConnectionIO[IO[Response[IO]]] =
      for
        clientOpt ← oauthRepository.findClient(clientId, clientSecret)
        response  ← clientOpt.foldTraverseR(
          ifEmpty = clientNotFound)(
          f       = removeOldTokens
        )
      yield response

    def removeOldTokens(client: ClientEntity): ConnectionIO[IO[Response[IO]]] =
      for
        _        ← oauthRepository.deleteRefreshToken(client.clientId)
        _        ← oauthRepository.deletePlatformAccessTokenWithScopes(client.clientId)
        response ← generateRefreshToken(client)
      yield response

    def generateRefreshToken(client: ClientEntity): ConnectionIO[IO[Response[IO]]] =
      for
        refreshTokenRes ← oauthRepository.newRefreshToken(
          clientId     = client.clientId,
          clientSecret = client.clientSecret
        )

        response ← refreshTokenRes.foldTraverseR(
          fa = _ ⇒ somethingWentWrong)(
          fb = generateAccessToken
        )
      yield response

    def generateAccessToken(refreshToken: TokenEntity): ConnectionIO[IO[Response[IO]]] =
      for accessTokenRes ← oauthRepository.newPlatformAccessToken(refreshToken)
        yield accessTokenRes.fold(
          fa = _ ⇒ somethingWentWrong,
          fb = accessToken ⇒
            tokensGenerated(
              accessToken  = accessToken.entity,
              refreshToken = refreshToken,
              redirectUrl  = redirectUrl getOrElse DefaultRedirect
            )
      )

    retrieveCredentials() flatTransact appModule.transactor
