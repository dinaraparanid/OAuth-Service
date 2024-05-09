package com.paranid5.auth_service.routing.auth

import cats.data.Reader
import cats.effect.IO
import cats.syntax.all.*

import com.paranid5.auth_service.data.user.entity.User
import com.paranid5.auth_service.domain.generateClientSecret
import com.paranid5.auth_service.routing.AppHttpResponse
import com.paranid5.auth_service.routing.auth.entity.{SignUpRequest, SignUpResponse}

import io.circe.syntax.*

import org.http4s.circe.CirceEntityCodec.{circeEntityEncoder, circeEntityDecoder}
import org.http4s.{Request, Response}
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
 * Adds new user and client to the database,
 * if it was not already registered.
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
 * 1. [[BadRequest]] if user was previously registered
 *
 * 2. [[InternalServerError]] in case of insertions errors
 *
 * 3. [[Created]] with credentials body:
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

    for
      request        ← query.as[SignUpRequest]
      encodedRequest = request.withEncodedPassword
      user           ← userRepository.getUserByEmail(encodedRequest.email)
      response       ← processUserData(user, encodedRequest)
    yield response
