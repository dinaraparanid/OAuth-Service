package com.paranid5.auth_service.routing.app

import cats.data.Reader
import cats.effect.IO

import com.paranid5.auth_service.routing.*
import com.paranid5.auth_service.routing.app.entity.UpdateRequest
import com.paranid5.auth_service.routing.app.response.*

import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import org.http4s.dsl.io.*
import org.http4s.{DecodeResult, Request, Response}

/**
 * Updates application's metadata.
 *
 * ==Route==
 * PATCH /app
 *
 * ==Body==
 * {{{
 *   {
 *     "app_id":        123
 *     "app_secret":    "abcd"
 *     "app_name":      "App Title",              // non-empty string
 *     "app_thumbnail": "https://some_image.png", // optional
 *     "callback_url":  "https://...",             // optional
 *   }
 * }}}
 *
 * ==Response==
 * 1. [[BadRequest]] - "Invalid body"
 *
 * 2. [[NotFound]] - "App was not found"
 *
 * 3. [[BadRequest]] - "App name must not be empty"
 *
 * 4. [[Ok]] - "App successfully updated"
 */

private def onUpdate(query: Request[IO]): AppHttpResponse =
  Reader: appModule ⇒
    val oauthRepository = appModule.oauthModule.oauthRepository

    def processRequest(requestRes: DecodeResult[IO, UpdateRequest]): IO[Response[IO]] =
      for
        responseIO ← requestRes.fold(_ ⇒ invalidBody, validateRequest)
        response   ← responseIO
      yield response

    def validateRequest(request: UpdateRequest): IO[Response[IO]] =
      for
        oldAppOpt ← oauthRepository.getApp(request.appId, request.appSecret)
        response  ← oldAppOpt.fold(
          ifEmpty = appNotFound)(
          f       = _ ⇒ if request.appName.isEmpty then appNameMustNotBeEmpty
                        else updateApp(request)
        )
      yield response

    def updateApp(request: UpdateRequest): IO[Response[IO]] =
      for
        appId ← oauthRepository.updateApp(
          appId           = request.appId,
          newAppName      = request.appName,
          newAppThumbnail = request.appThumbnail,
          newCallbackUrl  = request.callbackUrl,
        )

        response ← appSuccessfullyUpdated
      yield response

    processRequest(query.attemptAs[UpdateRequest])
