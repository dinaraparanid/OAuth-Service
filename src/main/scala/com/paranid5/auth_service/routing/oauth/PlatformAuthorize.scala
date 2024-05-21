package com.paranid5.auth_service.routing.oauth

import cats.data.Reader
import cats.effect.IO

import com.paranid5.auth_service.data.oauth.token.entity.AccessToken
import com.paranid5.auth_service.routing.oauth.entity.AuthorizeRequest
import com.paranid5.auth_service.routing.*

import doobie.syntax.all.*

import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import org.http4s.dsl.io.*
import org.http4s.{DecodeResult, Request, Response}

/**
 * Validates access token for authorization for platform.
 * Redirects either to provided callback url or the [[DefaultRedirect]]
 *
 * ==Route==
 * POST /oauth/authorize?client_id=123&redirect_url=https://...
 *
 * ==Body==
 * {{{
 *   {
 *     "token": "abcdef" // 45-th length string
 *   }
 * }}}
 *
 * ==Response==
 * 1. [[BadRequest]] - "Invalid body"
 *
 *
 * 2. [[NotFound]] - "Token was not found"
 *
 * 3. [[Forbidden]] - "Token has expired"
 *
 * 5. [[Found]] with provided callback url or with [[DefaultRedirect]]
 */

private def onPlatformAuthorize(
  query:       Request[IO],
  clientId:    Long,
  redirectUrl: Option[String]
): AppHttpResponse =
  Reader: appModule ⇒
    val oauthRepository = appModule.oauthModule.oauthRepository

    def processRequest(requestRes: DecodeResult[IO, AuthorizeRequest]): IO[Response[IO]] =
      for
        responseIO ← requestRes.fold(_ ⇒ invalidBody, retrieveToken)
        response   ← responseIO
      yield response

    def retrieveToken(request: AuthorizeRequest): IO[Response[IO]] =
      for
        tokenOpt ← oauthRepository
          .getPlatformClientAccessToken(clientId)
          .transact(appModule.transcactor)

        response ← processToken(request.token, tokenOpt)
      yield response

    def processToken(
      requestToken:      String,
      retrievedTokenOpt: Option[AccessToken]
    ): IO[Response[IO]] =
      retrievedTokenOpt.fold(
        ifEmpty = tokenNotFound)(
        f       = token ⇒ validateToken(token)
      )

    def validateToken(token: AccessToken): IO[Response[IO]] =
      for
        isValid ← oauthRepository.isTokenValid(
          clientId   = token.entity.clientId,
          tokenValue = token.entity.value
        ).transact(appModule.transcactor)

        response ← isValid.fold(
          fa = invalidToken,
          fb = _ ⇒ redirectToCallbackUrl(redirectUrl getOrElse DefaultRedirect)
        )
      yield response

    processRequest(query.attemptAs[AuthorizeRequest])
