package com.paranid5.auth_service.routing.oauth

import cats.data.Reader
import cats.effect.IO

import com.paranid5.auth_service.data.oauth.client.entity.{AppEntity, ClientEntity}
import com.paranid5.auth_service.data.oauth.token.entity.{AccessToken, TokenEntity}
import com.paranid5.auth_service.data.oauth.token.error.InvalidTokenReason
import com.paranid5.auth_service.routing.*
import com.paranid5.auth_service.routing.oauth.response.tokensGenerated
import com.paranid5.auth_service.utills.extensions.ApplicativeEitherOps.foldTraverseR
import com.paranid5.auth_service.utills.extensions.ApplicativeOptionOps.foldTraverseR
import com.paranid5.auth_service.utills.extensions.flatTransact

import doobie.free.connection.ConnectionIO

import org.http4s.Response
import org.http4s.dsl.io.*

/**
 * Updates both refresh and access tokens for the given client ap only
 * by removing old tokens and generating new ones.
 *
 * ==Route==
 * POST /oauth/token?client_id=123&client_secret=secret&app_id=123&app_secret=secret&redirect_url=https://...
 *
 * ==Response==
 * 1. [[BadRequest]] - "Invalid body"
 *
 * 2. [[NotFound]] - "Client was not found"
 *
 * 3. [[NotFound]] - "App was not found"
 *
 * 4. [[Created]] with both access and refresh tokens and redirect url:
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

private def onAppToken(
  clientId:     Long,
  clientSecret: String,
  appId:        Long,
  appSecret:    String,
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
          f       = retrieveApp
        )
      yield response

    def retrieveApp(client: ClientEntity): ConnectionIO[IO[Response[IO]]] =
      for
        appOpt   ← oauthRepository.getApp(appId, appSecret)
        response ← appOpt.foldTraverseR(
          ifEmpty = appNotFound)(
          f       = removeOldTokens(client, _)
        )
      yield response

    def removeOldTokens(
      client: ClientEntity,
      app:    AppEntity,
    ): ConnectionIO[IO[Response[IO]]] =
      for
        _ ← oauthRepository.deleteRefreshToken(client.clientId)

        _ ← oauthRepository.deleteAccessTokenWithScopes(
          clientId = client.clientId,
          appId    = Option(appId)
        )

        response ← generateRefreshToken(client, app)
      yield response

    def generateRefreshToken(
      client: ClientEntity,
      app:    AppEntity,
    ): ConnectionIO[IO[Response[IO]]] =
      for
        refreshTokenRes ← oauthRepository.newRefreshToken(
          clientId     = client.clientId,
          clientSecret = client.clientSecret
        )

        response ← refreshTokenRes.foldTraverseR(
          fa = _ ⇒ somethingWentWrong)(
          fb = generateAccessToken(client, app, _)
        )
      yield response

    def generateAccessToken(
      client:       ClientEntity,
      app:          AppEntity,
      refreshToken: TokenEntity,
    ): ConnectionIO[IO[Response[IO]]] =
      def response(
        accessTokenRes: Either[InvalidTokenReason, AccessToken],
        redirect:       String
      ): IO[Response[IO]] =
        accessTokenRes.fold(
          fa = _ ⇒ somethingWentWrong,
          fb = accessToken ⇒
            tokensGenerated(
              accessToken  = accessToken.entity,
              refreshToken = refreshToken,
              redirectUrl  = redirect
            )
        )

      val redirect = redirectUrl getOrElse (app.callbackUrl getOrElse DefaultRedirect)

      for accessTokenRes ← oauthRepository.newAppAccessToken(
        refreshToken     = refreshToken,
        accessTokenAppId = appId,
      ) yield response(accessTokenRes, redirect)

    retrieveCredentials() flatTransact appModule.transcactor
