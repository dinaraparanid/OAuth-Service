package com.paranid5.auth_service.routing.auth

import cats.data.Reader
import cats.effect.IO
import cats.syntax.all.*

import com.paranid5.auth_service.data.user.entity.User
import com.paranid5.auth_service.routing.*
import com.paranid5.auth_service.routing.auth.entity.{SignInRequest, matches}
import com.paranid5.auth_service.routing.auth.response.userSignedIn

import doobie.free.connection.ConnectionIO

import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import org.http4s.dsl.io.*
import org.http4s.{Request, Response}

/**
 * Sign in on platform (e.g. to manage user apps).
 * Retrieves client credentials of the given user
 *
 * ==Route==
 * POST /auth/sign_in
 *
 * ==Body==
 * {{{
 *   {
 *     "email":    "test@gmail.com",
 *     "password": "qwerty"
 *   }
 * }}}
 *
 * ==Response==
 * 1. [[BadRequest]] - "Invalid body"
 *
 * 2. [[NotFound]] - "User with provided email was not found"
 *
 * 3. [[NotFound]] - "User with provided email and password was not found"
 *
 * 4. [[Ok]] with credentials body:
 * {{{
 *   {
 *     "client_id":     123,
 *     "client_secret": "abcdefghij"
 *   }
 * }}}
 */

private def onSignIn(query: Request[IO]): AppHttpResponse =
  Reader: appModule ⇒
    val userRepository  = appModule.userModule.userRepository
    val oauthRepository = appModule.oauthModule.oauthRepository

    def retrieveUserData(request: SignInRequest): ConnectionIO[IO[Response[IO]]] =
      for
        user     ← userRepository.getUserByEmail(request.email)
        response ← processUserData(request.withEncodedPassword, user)
      yield response

    def processUserData(
      request:   SignInRequest,
      foundUser: Option[User]
    ): ConnectionIO[IO[Response[IO]]] =
      foundUser
        .toRight(())
        .map(validateUser(request, _))
        .sequence
        .map(_ getOrElse wrongEmail)

    def validateUser(
      request:   SignInRequest,
      foundUser: User
    ): ConnectionIO[IO[Response[IO]]] =
      def impl: Either[Unit, ConnectionIO[IO[Response[IO]]]] =
        if request matches foundUser then Right(retrieveCredentials(foundUser))
        else Left(())

      impl.sequence map (_ getOrElse wrongPassword)

    def retrieveCredentials(foundUser: User): ConnectionIO[IO[Response[IO]]] =
      for clientOpt ← oauthRepository.getClient(foundUser.userId)
        yield clientOpt.fold(
          ifEmpty = somethingWentWrong)(
          f       = cred ⇒ userSignedIn(cred.clientId, cred.clientSecret)
        )

    processRequest(query.attemptAs[SignInRequest])(retrieveUserData) run appModule
