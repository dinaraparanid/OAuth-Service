package com.paranid5.auth_service.routing.auth

import cats.data.Reader
import cats.effect.IO

import com.paranid5.auth_service.data.oauth.client.entity.ClientEntity
import com.paranid5.auth_service.data.user.entity.EmailConfirmCode
import com.paranid5.auth_service.routing.AppHttpResponse
import com.paranid5.auth_service.routing.auth.response.*
import com.paranid5.auth_service.utills.extensions.ApplicativeOptionOps.foldTraverseR

import doobie.free.connection.ConnectionIO
import doobie.syntax.all.*

import org.http4s.Response
import org.http4s.dsl.io.*

/**
 * Confirms email that was previously
 * assigned to client credentials.
 * Proceeds with email confirmation.
 * Removes confirmation code from the database
 * in case of success
 *
 * ==Route==
 * POST /auth/confirm_email?code=some_code
 *
 * ==Response==
 * 1. [[NotFound]] - "Invalid confirm code"
 *
 * 2. [[NotFound]] - "User was not found"
 *
 * 3. [[Created]] with credentials body:
 * {{{
 *   {
 *     "client_id":     123,
 *     "client_secret": "abcdefghij"
 *   }
 * }}}
 */

private def onConfirmEmail(code: String): AppHttpResponse =
  Reader: appModule ⇒
    val userRepository  = appModule.userModule.userRepository
    val oauthRepository = appModule.oauthModule.oauthRepository

    def findConfirmCode: ConnectionIO[IO[Response[IO]]] =
      for
        codeOpt ← userRepository.findConfirmationCode(code)
        response ← codeOpt.foldTraverseR(
          ifEmpty = invalidConfirmCode)(
          f       = findUser
        )
      yield response

    def findUser(emailConfirmCode: EmailConfirmCode): ConnectionIO[IO[Response[IO]]] =
      for
        userOpt  ← userRepository.getUserByEmail(emailConfirmCode.email)
        response ← userOpt.foldTraverseR(
          ifEmpty = userNotFound)(
          f       = u ⇒ findClientCredentials(u.userId)
        )
      yield response

    def findClientCredentials(userId: Long): ConnectionIO[IO[Response[IO]]] =
      def response(clientOpt: Option[ClientEntity]): IO[Response[IO]] =
        clientOpt.fold(
          ifEmpty = userNotFound)(
          f       = c ⇒ emailIsConfirmed(
            clientId     = c.clientId,
            clientSecret = c.clientSecret
          )
        )

      for
        _         ← userRepository.removeConfirmationCode(code)
        clientOpt ← oauthRepository.getClient(clientId = userId)
      yield response(clientOpt)

    findConfirmCode
      .transact(appModule.transactor)
      .flatten
