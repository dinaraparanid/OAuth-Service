package com.paranid5.auth_service.routing.app

import cats.data.Reader
import cats.effect.IO

import com.paranid5.auth_service.routing.*
import com.paranid5.auth_service.routing.app.entity.AllRequest
import com.paranid5.auth_service.routing.app.response.*

import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import org.http4s.dsl.io.*
import org.http4s.{DecodeResult, Request, Response}

/**
 * Retrieves all applications' info by client's credentials
 *
 * ==Route==
 * GET /app/all
 *
 * ==Body==
 * {{{
 *   {
 *     "client_id":     123
 *     "client_secret": "abcd"
 *   }
 * }}}
 *
 * ==Response==
 * 1. [[BadRequest]] - "Invalid body"
 *
 * 3. [[Ok]] with client's apps' credentials:
 * {{{
 *   [
 *     {
 *       "app_id":        123,
 *       "app_secret":    "abcd",
 *       "app_name":      "App Title",              // non-empty string
 *       "app_thumbnail": "https://some_image.png", // nullable
 *       "callback_url":  "https://..."             // nullable
 *     },
 *   ...
 *   ]
 * }}}
 */

private def onAll(query: Request[IO]): AppHttpResponse =
  Reader: appModule ⇒
    val oauthRepository = appModule.oauthModule.oauthRepository

    def processRequest(requestRes: DecodeResult[IO, AllRequest]): IO[Response[IO]] =
      for
        responseIO ← requestRes.fold(_ ⇒ invalidBody, findApps)
        response   ← responseIO
      yield response

    def findApps(request: AllRequest): IO[Response[IO]] =
      for
        apps     ← oauthRepository.getClientApps(request.clientId)
        response ← clientApps(apps)
      yield response

    processRequest(query.attemptAs[AllRequest])
