package com.paranid5.auth_service.routing.app

import cats.data.Reader
import cats.effect.IO

import com.paranid5.auth_service.routing.*
import com.paranid5.auth_service.routing.app.response.*

import org.http4s.Response
import org.http4s.dsl.io.*

/**
 * Deletes application with all.
 * Responds with app's credentials (id and secret)
 *
 * ==Route==
 * DELETE /app?client_id=123&app_id=234&app_secret=secret
 *
 * ==Response==
 * 1. [[BadRequest]] - "Invalid body"
 *
 * 2. [[NotFound]] - "App was not found"
 *
 * 3. [[Ok]] - "App successfully deleted"
 */

private def onDelete(
  clientId:  Long,
  appId:     Long,
  appSecret: String,
): AppHttpResponse =
  Reader: appModule ⇒
    val oauthRepository = appModule.oauthModule.oauthRepository

    def validateRequest(): IO[Response[IO]] =
      for
        appOpt   ← oauthRepository.getApp(appId, appSecret)
        response ← appOpt.fold(ifEmpty = appNotFound)(_ ⇒ deleteApp())
      yield response

    def deleteApp(): IO[Response[IO]] =
      for
        _ ← oauthRepository.deleteApp(
          clientId  = clientId,
          appId     = appId,
          appSecret = appSecret,
        )

        response ← appSuccessfullyDeleted
      yield response

    validateRequest()
