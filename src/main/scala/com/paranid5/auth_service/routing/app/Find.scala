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
 * Retrieves application's info by its credentials
 *
 * ==Route==
 * GET /app?app_id=123&app_secret=secret
 *
 * ==Response==
 * 1. [[BadRequest]] - "Invalid body"
 *
 * 2. [[NotFound]] - "App was not found"
 *
 * 3. [[Ok]] with requested app's credentials:
 * {{{
 *   {
 *     "app_id":        123,
 *     "app_secret":    "abcd",
 *     "app_name":      "App Title",              // non-empty string
 *     "app_thumbnail": "https://some_image.png", // nullable
 *     "callback_url":  "https://..."             // nullable
 *     "client_id":     234
 *   }
 * }}}
 */

private def onFind(
  appId:     Long,
  appSecret: String,
): AppHttpResponse =
  Reader: appModule ⇒
    val oauthRepository = appModule.oauthModule.oauthRepository

    def impl: ConnectionIO[IO[Response[IO]]] =
      for appOpt ← oauthRepository.getApp(
        appId = appId,
        appSecret = appSecret,
      ) yield appOpt.fold(
        ifEmpty = appNotFound)(
        f       = appSuccessfullyFound
      )

    impl flatTransact appModule.transcactor
