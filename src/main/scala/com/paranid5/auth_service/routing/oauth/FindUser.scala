package com.paranid5.auth_service.routing.oauth

import cats.data.Reader
import cats.effect.IO
import cats.syntax.all.*

import com.paranid5.auth_service.data.oauth.token.entity.TokenEntity
import com.paranid5.auth_service.routing.*
import com.paranid5.auth_service.routing.oauth.response.userMetadata
import org.http4s.Response
import org.http4s.dsl.io.*

/**
 * Retrieves user metadata by access token.
 *
 * ==Route==
 * GET /oauth/user?access_token=abcd
 *
 * ==Response==
 * 1. [[BadRequest]] - "Invalid body"
 *
 * 2. [[NotFound]] - "Token was not found"
 *
 * 3. [[NotFound]] - "Client was not found"
 *
 * 4. [[Ok]] with user metadata:
 * {{{
 *   {
 *     "client_id":     123,
 *     "client_secret": "secret",
 *     "username":      "user",
 *     "email":         "email"
 *   }
 * }}}
 */

private def onFindUser(accessToken: String): AppHttpResponse =
  Reader: appModule ⇒
    val userRepository  = appModule.userModule.userRepository
    val oauthRepository = appModule.oauthModule.oauthRepository

    def validateRequest(): IO[Response[IO]] =
      for
        tokenRes ← oauthRepository.retrieveToken(accessToken)
        response ← tokenRes.fold(_ ⇒ tokenNotFound, retrieveUser)
      yield response

    def retrieveUser(token: TokenEntity): IO[Response[IO]] =
      for
        userOpt       ← userRepository.getUser(userId = token.clientId)
        clientOpt     ← oauthRepository.getClient(clientId = token.clientId)
        userClientOpt = (userOpt, clientOpt) mapN ((user, client) ⇒ (user, client))
        response      ← userClientOpt.fold(
          ifEmpty = clientNotFound)(
          f       = (user, client) ⇒ userMetadata(user, client)
        )
      yield response

    validateRequest()
