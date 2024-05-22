package com.paranid5.auth_service.routing.app

import cats.data.Reader
import cats.effect.IO

import com.paranid5.auth_service.routing.*
import com.paranid5.auth_service.routing.app.response.*
import com.paranid5.auth_service.utills.extensions.flatTransact

import doobie.free.connection.ConnectionIO

import org.http4s.Response
import org.http4s.dsl.io.*

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

    def response: ConnectionIO[IO[Response[IO]]] =
      for apps ← oauthRepository.getClientApps(clientId)
        yield clientApps(apps)

    response flatTransact appModule.transcactor
