package com.paranid5.auth_service.routing.auth

import cats.data.Reader
import cats.effect.IO
import cats.syntax.all.*

import com.paranid5.auth_service.data.user.entity.User
import com.paranid5.auth_service.domain.generateClientSecret
import com.paranid5.auth_service.routing.{AppHttpResponse, invalidBody}
import com.paranid5.auth_service.routing.auth.entity.{SignUpRequest, SignUpResponse}

import io.circe.syntax.*

import org.http4s.circe.CirceEntityCodec.{circeEntityDecoder, circeEntityEncoder}
import org.http4s.{DecodeResult, Request, Response}
import org.http4s.dsl.io.*

private def userAlreadyRegistered: IO[Response[IO]] =
  BadRequest("User with such email is already registered")

private def userSuccessfullyRegistered(
  clientId:     Long,
  clientSecret: String,
): IO[Response[IO]] =
  Created(SignUpResponse(clientId, clientSecret).asJson)

private def credentialsGenerationError: IO[Response[IO]] =
  InternalServerError("User credentials generation error. Try again")

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
 *     "client_secret": "abcdefghij" // 10-th length string
 *   }
 * }}}
 */

private def onSignUp(query: Request[IO]): AppHttpResponse =
  Reader: appModule ⇒
    val userRepository  = appModule.userModule.userRepository
    val oauthRepository = appModule.oauthModule.oauthRepository

    def processRequest(requestRes: DecodeResult[IO, SignUpRequest]): IO[Response[IO]] =
      for
        responseIO ← requestRes.fold(_ ⇒ invalidBody, retrieveUserData)
        response   ← responseIO
      yield response

    def retrieveUserData(request: SignUpRequest): IO[Response[IO]] =
      val encodedRequest = request.withEncodedPassword

      for
        user     ← userRepository.getUserByEmail(encodedRequest.email)
        response ← processUserData(user, encodedRequest)
      yield response

    def processUserData(
      foundUser:       Option[User],
      encodedUserData: SignUpRequest,
    ): IO[Response[IO]] =
      foundUser.fold(
        ifEmpty = addNewUser(encodedUserData))(
        f       = _ ⇒ userAlreadyRegistered
      )

    def addNewUser(encodedUserData: SignUpRequest): IO[Response[IO]] =
      for
        clientId ← userRepository.storeUser(
          username        = encodedUserData.username,
          email           = encodedUserData.email,
          encodedPassword = encodedUserData.password
        )

        clientSecretRes ← generateClientSecret
        response        ← processGeneratedCredentials(clientId, clientSecretRes)
      yield response

    def processGeneratedCredentials(
      clientId:     Long,
      clientSecret: Either[Throwable, String]
    ): IO[Response[IO]] =
      val genCredentials = clientSecret map ((clientId, _))

      for
        insertionRes ← clientSecret
          .map: clientSecret ⇒
            oauthRepository.insertClient(clientId, clientSecret)
              *> IO((clientId, clientSecret))
          .sequence

        response ← insertionRes.fold(
          fa = _                        ⇒ credentialsGenerationError,
          fb = (clientId, clientSecret) ⇒ userSuccessfullyRegistered(clientId, clientSecret)
        )
      yield response

    processRequest(query.attemptAs[SignUpRequest])
