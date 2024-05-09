package com.paranid5.auth_service.routing.auth

import cats.data.Reader
import cats.effect.IO

import com.paranid5.auth_service.data.user.entity.User
import com.paranid5.auth_service.routing.{AppHttpResponse, invalidBody}
import com.paranid5.auth_service.routing.auth.entity.{SignInRequest, SignInResponse}

import io.circe.syntax.*

import org.http4s.circe.CirceEntityCodec.{circeEntityDecoder, circeEntityEncoder}
import org.http4s.dsl.io.*
import org.http4s.{DecodeResult, Request, Response}

private def wrongEmail: IO[Response[IO]] =
  NotFound("User with provided email was not found")

private def wrongPassword: IO[Response[IO]] =
  NotFound("User with provided email and password was not found")

private def userSignedIn(
  clientId:     Long,
  clientSecret: String,
): IO[Response[IO]] =
  Ok(SignInResponse(clientId, clientSecret).asJson)

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
 *     "client_secret": "abcdefghij" // 10-th length string
 *   }
 * }}}
 */

private def onSignIn(query: Request[IO]): AppHttpResponse =
  Reader: appModule ⇒
    val userRepository  = appModule.userModule.userRepository
    val oauthRepository = appModule.oauthModule.oauthRepository

    def processRequest(requestRes: DecodeResult[IO, SignInRequest]): IO[Response[IO]] =
      for
        responseIO ← requestRes.fold(_ ⇒ invalidBody, retrieveUserData)
        response   ← responseIO
      yield response

    def retrieveUserData(request: SignInRequest): IO[Response[IO]] =
      for
        user     ← userRepository.getUserByEmail(request.email)
        response ← processUserData(user)
      yield response

    def processUserData(foundUser: Option[User]): IO[Response[IO]] =
      foundUser.fold(
        ifEmpty = wrongEmail)(
        f       = retrieveCredentials
      )

    def retrieveCredentials(foundUser: User): IO[Response[IO]] =
      for
        clientOpt ← oauthRepository.findClient(foundUser.userId, foundUser.encodedPassword)
        response  ← clientOpt.fold(
          ifEmpty = wrongPassword)(
          f       = cred ⇒ userSignedIn(cred.clientId, cred.clientSecret)
        )
      yield response

    processRequest(query.attemptAs[SignInRequest])
