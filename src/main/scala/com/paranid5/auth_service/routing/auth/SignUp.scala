package com.paranid5.auth_service.routing.auth

import cats.data.Reader
import cats.effect.IO

import com.paranid5.auth_service.data.mail.sendConfirmEmailViaMailer
import com.paranid5.auth_service.data.user.entity.User
import com.paranid5.auth_service.domain.generateSecret
import com.paranid5.auth_service.routing.*
import com.paranid5.auth_service.routing.auth.entity.SignUpRequest
import com.paranid5.auth_service.routing.auth.response.*
import com.paranid5.auth_service.utills.extensions.ApplicativeEitherOps.foldTraverseR
import com.paranid5.auth_service.utills.extensions.ApplicativeOptionOps.foldTraverseL

import doobie.free.connection.ConnectionIO

import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import org.http4s.dsl.io.*
import org.http4s.{Request, Response}

/**
 * Sign up on platform (e.g. to manage user apps).
 * Proceeds with email confirmation.
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
 *     "password": "qwerty",
 *     "confirm_url": "https://platform_frontent/confirm_email
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
 * 4. [[Ok]] - "Confirmation email was successfully sent"
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
      foundUser.foldTraverseL(
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
        response        ← processGeneratedCredentials(
          clientId        = clientId,
          clientSecretRes = clientSecretRes,
          encodedUserData = encodedUserData
        )
      yield response

    def processGeneratedCredentials(
      clientId:        Long,
      clientSecretRes: Either[Throwable, String],
      encodedUserData: SignUpRequest,
    ): ConnectionIO[IO[Response[IO]]] =
      clientSecretRes.foldTraverseR(
        fa = _ ⇒ credentialsGenerationError)(
        fb = clientSecret ⇒
          oauthRepository
            .insertClient(clientId, clientSecret)
            .flatMap(_ ⇒ generateConfirmationCode(encodedUserData))
      )

    def generateConfirmationCode(encodedUserData: SignUpRequest): ConnectionIO[IO[Response[IO]]] =
      generateSecret[ConnectionIO] flatMap:
        _.foldTraverseR(
          _ ⇒ confirmationCodeGenerationError)(
          updateConfirmationCode(encodedUserData, _)
        )

    def updateConfirmationCode(
      encodedUserData: SignUpRequest,
      confirmCode:     String
    ): ConnectionIO[IO[Response[IO]]] =
      for _ ← userRepository.updateConfirmationCode(
        email = encodedUserData.email,
        confirmationCode = confirmCode,
      ) yield sendConfirmEmail(encodedUserData, confirmCode)

    def sendConfirmEmail(
      encodedUserData: SignUpRequest,
      confirmCode:     String
    ): IO[Response[IO]] =
      sendConfirmEmailViaMailer(
        username = encodedUserData.username,
        email = encodedUserData.email,
        confirmationUrl = encodedUserData.confirmUrl,
        confirmationCode = confirmCode,
      )
        .run(appModule)
        .flatMap:
          _.fold(
            _ ⇒ confirmationEmailNotSent,
            _ ⇒ confirmationEmailSuccessfullySent
          )

    processRequest(query.attemptAs[SignUpRequest])(retrieveUserData) run appModule
