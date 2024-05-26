package com.paranid5.auth_service.routing.app

import cats.data.Reader
import cats.effect.IO

import com.paranid5.auth_service.routing.*
import com.paranid5.auth_service.routing.app.response.*
import com.paranid5.auth_service.utills.extensions.ApplicativeEitherOps.foldTraverseR
import com.paranid5.auth_service.utills.extensions.ApplicativeOptionOps.foldTraverseR
import com.paranid5.auth_service.utills.extensions.flatTransact

import doobie.free.connection.ConnectionIO

import org.http4s.Response
import org.http4s.dsl.io.*

/**
 * Updates application's metadata.
 *
 * ==Route==
 * PATCH /app?app_id=123&app_secret=secret&app_name=name&app_thumbnail=https://image.png&redirect_url=https://...
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

private def onUpdate(
  appId:        Long,
  appSecret:    String,
  appName:      String,
  appThumbnail: Option[String],
  redirectUrl:  Option[String]
): AppHttpResponse =
  Reader: appModule ⇒
    val oauthRepository = appModule.oauthModule.oauthRepository

    def validateRequest(): ConnectionIO[IO[Response[IO]]] =
      def response: ConnectionIO[IO[Response[IO]]] =
        val either = if appName.isEmpty then Left(()) else Right(())
        either.foldTraverseR(_ ⇒ appNameMustNotBeEmpty)(_ ⇒ updateApp())

      for
        oldAppOpt ← oauthRepository.getApp(appId, appSecret)
        response  ← oldAppOpt.foldTraverseR(
          ifEmpty = appNotFound)(
          f       = _ ⇒ response
        )
      yield response

    def updateApp(): ConnectionIO[IO[Response[IO]]] =
      for appId ← oauthRepository.updateApp(
        appId           = appId,
        newAppName      = appName,
        newAppThumbnail = appThumbnail,
        newCallbackUrl  = redirectUrl,
      ) yield appSuccessfullyUpdated

    validateRequest() flatTransact appModule.transactor
