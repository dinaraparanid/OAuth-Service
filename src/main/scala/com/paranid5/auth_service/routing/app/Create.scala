package com.paranid5.auth_service.routing.app

import cats.data.Reader
import cats.effect.IO

import com.paranid5.auth_service.domain.generateSecret
import com.paranid5.auth_service.routing.*
import com.paranid5.auth_service.routing.app.entity.CreateRequest
import com.paranid5.auth_service.routing.app.response.*

import doobie.syntax.all.*

import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import org.http4s.dsl.io.*
import org.http4s.{DecodeResult, Request, Response}

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
 *     "app_secret": "abcd" // 10-th length string
 *   }
 * }}}
 */

private def onCreate(query: Request[IO]): AppHttpResponse =
  Reader: appModule ⇒
    val oauthRepository = appModule.oauthModule.oauthRepository

    def processRequest(requestRes: DecodeResult[IO, CreateRequest]): IO[Response[IO]] =
      for
        responseIO ← requestRes.fold(_ ⇒ invalidBody, validateRequest)
        response   ← responseIO
      yield response

    def validateRequest(request: CreateRequest): IO[Response[IO]] =
      if request.appName.isEmpty then appNameMustNotBeEmpty
      else generateCredentials(request)

    def generateCredentials(request: CreateRequest): IO[Response[IO]] =
      for
        appSecretRes ← generateSecret[IO]
        response     ← appSecretRes.fold(
          fa = _ ⇒ credentialsGenerationError,
          fb = createApp(request, _)
        )
      yield response

    def createApp(request: CreateRequest, appSecret: String): IO[Response[IO]] =
      for
        appId ← oauthRepository.insertApp(
          appSecret    = appSecret,
          appName      = request.appName,
          appThumbnail = request.appThumbnail,
          callbackUrl  = request.callbackUrl,
          clientId     = request.clientId
        ).transact(appModule.transcactor)

        response ← appSuccessfullyCreated(appId, appSecret)
      yield response

    processRequest(query.attemptAs[CreateRequest])
