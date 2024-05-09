package com.paranid5.auth_service.routing.app

import cats.data.Reader
import cats.effect.IO

import com.paranid5.auth_service.routing.*
import com.paranid5.auth_service.routing.app.entity.FindRequest
import com.paranid5.auth_service.routing.app.response.*

import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import org.http4s.dsl.io.*
import org.http4s.{DecodeResult, Request, Response}

/**
 * Retrieves application's info by its credentials
 *
 * ==Route==
 * GET /app
 *
 * ==Body==
 * {{{
 *   {
 *     "app_id":     123
 *     "app_secret": "abcd" // 10-th length string
 *   }
 * }}}
 *
 * ==Response==
 * 1. [[BadRequest]] - "Invalid body"
 *
 * 2. [[NotFound]] - "App was not found"
 *
 * 3. [[Created]] with created app's credentials:
 * {{{
 *   {
 *     "app_id":        123,
 *     "app_secret":    "abcd",                   // 10-th length string
 *     "app_name":      "App Title",              // non-empty string
 *     "app_thumbnail": "https://some_image.png", // nullable
 *     "callback_url":  "https://..."             // nullable
 *     "client_id":     234
 *   }
 * }}}
 */

private def onFind(query: Request[IO]): AppHttpResponse =
  Reader: appModule ⇒
    val oauthRepository = appModule.oauthModule.oauthRepository

    def processRequest(requestRes: DecodeResult[IO, FindRequest]): IO[Response[IO]] =
      for
        responseIO ← requestRes.fold(_ ⇒ invalidBody, findApp)
        response   ← responseIO
      yield response

    def findApp(request: FindRequest): IO[Response[IO]] =
      for
        appOpt ← oauthRepository.getApp(
          appId     = request.appId,
          appSecret = request.appSecret,
        )

        response ← appOpt.fold(ifEmpty = appNotFound)(f = appSuccessfullyFound)
      yield response

    processRequest(query.attemptAs[FindRequest])
