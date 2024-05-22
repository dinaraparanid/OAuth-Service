package com.paranid5.auth_service.routing.oauth

import cats.data.Reader
import cats.effect.IO
import cats.syntax.all.*

import com.paranid5.auth_service.data.oauth.client.entity.ClientEntity
import com.paranid5.auth_service.data.oauth.token.entity.TokenEntity
import com.paranid5.auth_service.data.user.entity.User
import com.paranid5.auth_service.routing.*
import com.paranid5.auth_service.routing.oauth.response.userMetadata
import com.paranid5.auth_service.utills.extensions.ApplicativeEitherOps.foldTraverseR
import com.paranid5.auth_service.utills.extensions.flatTransact

import doobie.free.connection.ConnectionIO

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

    def validateRequest(): ConnectionIO[IO[Response[IO]]] =
      for
        tokenRes ← oauthRepository.retrieveToken(accessToken)
        response ← tokenRes.foldTraverseR(_ ⇒ tokenNotFound)(retrieveUser)
      yield response

    def retrieveUser(token: TokenEntity): ConnectionIO[IO[Response[IO]]] =
      def response(userClientOpt: Option[(User, ClientEntity)]): IO[Response[IO]] =
        userClientOpt.fold(
          ifEmpty = clientNotFound)(
          f = (user, client) ⇒ userMetadata(user, client)
        )

      for
        userOpt       ← userRepository.getUser(userId = token.clientId)
        clientOpt     ← oauthRepository.getClient(clientId = token.clientId)
        userClientOpt = (userOpt, clientOpt) mapN ((user, client) ⇒ (user, client))
      yield response(userClientOpt)

    validateRequest() flatTransact appModule.transcactor
