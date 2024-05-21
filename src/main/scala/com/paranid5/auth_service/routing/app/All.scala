package com.paranid5.auth_service.routing.app

import cats.data.Reader
import cats.effect.IO

import com.paranid5.auth_service.routing.*
import com.paranid5.auth_service.routing.app.entity.AllRequest
import com.paranid5.auth_service.routing.app.response.*

import doobie.syntax.all.*

import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import org.http4s.dsl.io.*
import org.http4s.{DecodeResult, Request, Response}

/**
 * Retrieves all applications' info by client's credentials
 *
 * ==Route==
 * GET /app/all?&client_id=123
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

private def onAll(clientId: Long): AppHttpResponse =
  Reader: appModule ⇒
    val oauthRepository = appModule.oauthModule.oauthRepository

    for
      apps ← oauthRepository
        .getClientApps(clientId)
        .transact(appModule.transcactor)

      response ← clientApps(apps)
    yield response
