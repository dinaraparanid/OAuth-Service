package com.paranid5.auth_service.routing.auth

import cats.data.Reader
import cats.effect.IO

import com.paranid5.auth_service.data.user.entity.User
import com.paranid5.auth_service.domain.generateSecret
import com.paranid5.auth_service.routing.*
import com.paranid5.auth_service.routing.auth.entity.SignUpRequest
import com.paranid5.auth_service.routing.auth.response.userSuccessfullyRegistered
import com.paranid5.auth_service.utills.extensions.*

import doobie.free.connection.ConnectionIO

import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import org.http4s.dsl.io.*
import org.http4s.{Request, Response}

/**
 * Sign up on platform (e.g. to manage user apps).
 * Adds new user and client to the database,
 * if it was not already registered.
 *
 * ==Route==
 * POST /auth/sign_up
 *
 * ==Body==
 * {{{
 *   {
 *     "username": "some username",
 *     "email":    "test@gmail.com",
 *     "password": "qwerty"
 *   }
 * }}}
 *
 * ==Response==
 * 1. [[BadRequest]] - "Invalid body"
 *
 * 2. [[BadRequest]] if user was previously registered
 *
 * 3. [[InternalServerError]] in case of insertions errors
 *
 * 4. [[Created]] with credentials body:
 * {{{
 *   {
 *     "client_id":     123,
 *     "client_secret": "abcdefghij"
 *   }
 * }}}
 */

private def onSignUp(query: Request[IO]): AppHttpResponse =
  Reader: appModule ⇒
    val userRepository  = appModule.userModule.userRepository
    val oauthRepository = appModule.oauthModule.oauthRepository

    def retrieveUserData(request: SignUpRequest): ConnectionIO[IO[Response[IO]]] =
      val encodedRequest = request.withEncodedPassword

      for
        user     ← userRepository.getUserByEmail(encodedRequest.email)
        response ← processUserData(user, encodedRequest)
      yield response

    def processUserData(
      foundUser:       Option[User],
      encodedUserData: SignUpRequest,
    ): ConnectionIO[IO[Response[IO]]] =
      foundUser.unwrapSequencedL(
        ifEmpty = addNewUser(encodedUserData))(
        f       = _ ⇒ userAlreadyRegistered
      )

    def addNewUser(encodedUserData: SignUpRequest): ConnectionIO[IO[Response[IO]]] =
      for
        clientId ← userRepository.storeUser(
          username        = encodedUserData.username,
          email           = encodedUserData.email,
          encodedPassword = encodedUserData.password
        )

        clientSecretRes ← generateSecret[ConnectionIO]
        response        ← processGeneratedCredentials(clientId, clientSecretRes)
      yield response

    def processGeneratedCredentials(
      clientId:        Long,
      clientSecretRes: Either[Throwable, String]
    ): ConnectionIO[IO[Response[IO]]] =
      clientSecretRes.foldSequencedR(
        fa = _ ⇒ credentialsGenerationError)(
        fb = clientSecret ⇒
          oauthRepository
            .insertClient(clientId, clientSecret)
            .map(_ ⇒ userSuccessfullyRegistered(clientId, clientSecret))
      )

    processRequest(query.attemptAs[SignUpRequest])(retrieveUserData) run appModule
