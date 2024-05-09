package com.paranid5.auth_service.routing.app

import cats.data.Reader
import cats.effect.IO

import com.paranid5.auth_service.routing.*
import com.paranid5.auth_service.routing.app.entity.DeleteRequest
import com.paranid5.auth_service.routing.app.response.*

import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import org.http4s.dsl.io.*
import org.http4s.{DecodeResult, Request, Response}

/**
 * Deletes application with all.
 * Responds with app's credentials (id and secret)
 *
 * ==Route==
 * DELETE /app
 *
 * ==Body==
 * {{{
 *   {
 *     "client_id":  123
 *     "app_id":     234,
 *     "app_secret": "abcd", // 10-th length string
 *   }
 * }}}
 *
 * ==Response==
 * 1. [[BadRequest]] - "Invalid body"
 *
 * 2. [[NotFound]] - "App was not found"
 *
 * 3. [[Ok]] - "App successfully deleted"
 */

private def onDelete(query: Request[IO]): AppHttpResponse =
  Reader: appModule ⇒
    val oauthRepository = appModule.oauthModule.oauthRepository

    def processRequest(requestRes: DecodeResult[IO, DeleteRequest]): IO[Response[IO]] =
      for
        responseIO ← requestRes.fold(_ ⇒ invalidBody, validateRequest)
        response   ← responseIO
      yield response

    def validateRequest(request: DeleteRequest): IO[Response[IO]] =
      for
        appOpt   ← oauthRepository.getApp(request.appId, request.appSecret)
        response ← appOpt.fold(ifEmpty = appNotFound)(_ ⇒ deleteApp(request))
      yield response

    def deleteApp(request: DeleteRequest): IO[Response[IO]] =
      for
        _ ← oauthRepository.deleteApp(
          clientId  = request.clientId,
          appId     = request.appId,
          appSecret = request.appSecret,
        )

        response ← appSuccessfullyDeleted
      yield response

    processRequest(query.attemptAs[DeleteRequest])
