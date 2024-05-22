package com.paranid5.auth_service.routing.app

import cats.data.Reader
import cats.effect.IO

import com.paranid5.auth_service.domain.generateSecret
import com.paranid5.auth_service.routing.*
import com.paranid5.auth_service.routing.app.entity.CreateRequest
import com.paranid5.auth_service.routing.app.response.*
import com.paranid5.auth_service.utills.extensions.ApplicativeEitherOps.foldTraverseR

import doobie.free.connection.ConnectionIO

import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import org.http4s.dsl.io.*
import org.http4s.{Request, Response}

/**
 * Creates new application.
 * Responds with app's credentials (id and secret)
 *
 * ==Route==
 * POST /app
 *
 * ==Body==
 * {{{
 *   {
 *     "client_id":     123
 *     "app_name":      "App Title",              // non-empty string
 *     "app_thumbnail": "https://some_image.png", // optional
 *     "callback_url":  "https://..."             // optional
 *   }
 * }}}
 *
 * ==Response==
 * 1. [[BadRequest]] - "Invalid body"
 *
 * 2. [[BadRequest]] - "App name must not be empty"
 *
 * 3. [[InternalServerError]] - "User credentials generation error. Try again"
 *
 * 4. [[Created]] with created app's credentials:
 * {{{
 *   {
 *     "app_id":     123,
 *     "app_secret": "abcd"
 *   }
 * }}}
 */

private def onCreate(query: Request[IO]): AppHttpResponse =
  Reader: appModule ⇒
    val oauthRepository = appModule.oauthModule.oauthRepository

    def validateRequest(request: CreateRequest): ConnectionIO[IO[Response[IO]]] =
      val either = if request.appName.isEmpty then Left(()) else Right(request)
      either.foldTraverseR(_ ⇒ appNameMustNotBeEmpty)(generateCredentials)

    def generateCredentials(request: CreateRequest): ConnectionIO[IO[Response[IO]]] =
      for
        appSecretRes ← generateSecret[ConnectionIO]
        response     ← appSecretRes.foldTraverseR(
          fa = _ ⇒ credentialsGenerationError)(
          fb = createApp(request, _)
        )
      yield response

    def createApp(
      request:   CreateRequest,
      appSecret: String
    ): ConnectionIO[IO[Response[IO]]] =
      for appId ← oauthRepository.insertApp(
        appSecret    = appSecret,
        appName      = request.appName,
        appThumbnail = request.appThumbnail,
        callbackUrl  = request.callbackUrl,
        clientId     = request.clientId
      ) yield appSuccessfullyCreated(appId, appSecret)

    processRequest(query.attemptAs[CreateRequest])(validateRequest) run appModule
